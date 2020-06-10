package org.opentorah.xml

import scala.xml.{Elem, Node}
import zio.IO

private[xml] sealed trait Content

private[xml] object Content {

  type Modifier[A] = Content => IO[Error, (Content, A)]

  private final case object Empty extends Content

  private final case class Characters(characters: Option[String]) extends Content

  private final case class Elements(nextElementNumber: Int, nodes: Seq[Node]) extends Content

  private final case class Mixed(nextElementNumber: Int, nodes: Seq[Node]) extends Content

  def open(nodes: Seq[Node], contentType: ContentType): IO[Error, Content] = {
    val (elements: Seq[Elem], nonElements: Seq[Node]) = partition(nodes)
    val characters: Option[String] = toCharacters(nonElements)

    contentType match {
      case ContentType.Empty =>
        if (elements.nonEmpty) IO.fail(s"Spurious elements: $elements")
        else if (characters.nonEmpty) IO.fail(s"Spurious characters: '${characters.get}'")
        else IO.succeed(Empty)

      case ContentType.Characters =>
        if (elements.nonEmpty) IO.fail(s"Spurious elements: $elements")
        else IO.succeed(Characters(characters))

      case ContentType.Elements =>
        if (characters.nonEmpty) IO.fail(s"Spurious characters: '${characters.get}'")
        else IO.succeed(Elements(0, nodes))

      case ContentType.Mixed =>
        IO.succeed(Mixed(0, nodes))
    }
  }

  val takeCharacters: Modifier[Option[String]] = {
    case Characters(characters) =>
      IO.succeed((Characters(None), characters))

    case Mixed(nextElementNumber, nodes) =>
      val (elements: Seq[Elem], nonElements: Seq[Node]) = partition(nodes)
      if (elements.nonEmpty) IO.fail(s"Elements in $this")
      else IO.succeed((Mixed(nextElementNumber, Seq.empty), toCharacters(nonElements)))

    case content =>
      IO.fail(s"No characters in $content")
  }

  val getNextElementName: Content => Option[String] = {
    case Elements(_, nodes) => getNextElementName(nodes)
    case Mixed(_, nodes) => getNextElementName(nodes)
    case _ => None
  }

  private def getNextElementName(nodes: Seq[Node]): Option[String] =
    nodes.dropWhile(Xml.isWhitespace).headOption.flatMap {
      case result: Elem => Some(result.label)
      case _ => None
    }

  def takeNextElement(p: Elem => Boolean): Modifier[Option[Elem]] = {
    case Elements(nextElementNumber, nodes) => IO.succeed {
      val (result: Option[Elem], newNodes: Seq[Node]) = takeNextElement(nodes, p)
      (Elements(nextElementNumber + (if (result.isEmpty) 0 else 1), newNodes), result)
    }

    case Mixed(nextElementNumber, nodes) => IO.succeed {
      val (result: Option[Elem], newNodes: Seq[Node]) = takeNextElement(nodes, p)
      (Mixed(nextElementNumber + (if (result.isEmpty) 0 else 1), newNodes), result)
    }

    case content => IO.fail(s"No element in $content")
  }

  private def takeNextElement(nodes: Seq[Node], p: Elem => Boolean): (Option[Elem], Seq[Node]) = {
    val noLeadingWhitespace = nodes.dropWhile(Xml.isWhitespace)
    noLeadingWhitespace.headOption.fold[(Option[Elem], Seq[Node])]((None, nodes)) {
      case result: Elem if p(result) => (Some(result), noLeadingWhitespace.tail)
      case _ => (None, nodes)
    }
  }

  val takeAllNodes: Modifier[Seq[Node]] = {
    case Elements(nextElementNumber: Int, nodes: Seq[Node]) =>
      IO.succeed(Elements(nextElementNumber, Seq.empty), nodes)

    case Mixed(nextElementNumber: Int, nodes: Seq[Node]) =>
      IO.succeed(Mixed(nextElementNumber, Seq.empty), nodes)

    case content => IO.fail(s"No nodes in $content")
  }

  val checkNoLeftovers: Content => IO[Error, Unit] = {
    case Empty => ok

    case Characters(characters) =>
      characters.fold[IO[Error, Unit]](ok)(characters => IO.fail(s"Unparsed characters: $characters"))

    case Elements(_, nodes) =>
      if (nodes.forall(Xml.isWhitespace)) ok else IO.fail(s"Unparsed elements: $nodes")

    case Mixed(_, nodes) =>
      if (nodes.forall(Xml.isWhitespace)) ok else IO.fail(s"Unparsed nodes: $nodes")
  }

  private def partition(nodes: Seq[Node]): (Seq[Elem], Seq[Node]) = {
    val (elems, nonElems) = nodes.partition(_.isInstanceOf[Elem])
    (elems.map(_.asInstanceOf[Elem]), nonElems)
  }

  private def toCharacters(nodes: Seq[Node]): Option[String] = {
    val result = nodes.map(_.text).mkString.trim
    if (result.isEmpty) None else Some(result)
  }

  private val ok: IO[Error, Unit] = IO.succeed(())
}

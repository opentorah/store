package org.digitaljudaica.xml

import cats.implicits._
import scala.xml.{Elem, Node}

private[xml] final class Current(
  from: Option[From],
  name: String,
  attributes: Map[String, String],
  elements: Seq[Elem],
  nextElementNumber: Int,
  characters: Option[String]
) {
  override def toString: String =
    s"$name, before #$nextElementNumber ($getNextNestedElementName)" + from.fold("")(url => s"  from [$url]")

  def getName: String = name

  def getFrom: Option[From] = from

  def takeAttribute(name: String): (Current, Option[String]) =
    (new Current(from, name, attributes - name, elements, nextElementNumber, characters), attributes.get(name))

  def takeCharacters: (Current, Option[String]) =
    (new Current(from, name, attributes, elements, nextElementNumber, characters = None), characters)

  def getNextNestedElementName: Option[String] =
    elements.headOption.map(_.label)

  def takeNextNestedElement: (Current, Elem) =
    (new Current(from, name, attributes, elements.tail, nextElementNumber + 1, characters), elements.head)

  def checkContent(charactersAllowed: Boolean): Parser[Unit] = for {
    _ <- Parser.check(elements.isEmpty || characters.isEmpty, s"Mixed content: [${characters.get}] $elements")
    _ <- Parser.check(characters.isEmpty || charactersAllowed, s"Characters are not allowed: ${characters.get}")
  } yield ()

  def checkNoLeftovers: Parser[Unit] = for {
    _ <- Parser.check(attributes.isEmpty, s"Unparsed attributes: $attributes")
    _ <- Parser.check(characters.isEmpty, s"Unparsed characters: ${characters.get}")
    _ <- Parser.check(elements.isEmpty, s"Unparsed elements: $elements")
  } yield ()
}

private[xml] object Current {

  def apply(from: Option[From], element: Elem): Current = {
    val (elements: Seq[Node], nonElements: Seq[Node]) = element.child.partition(_.isInstanceOf[Elem])
    new Current(
      from,
      name = element.label,
      attributes = element.attributes.map(metadata => metadata.key -> metadata.value.toString).toMap,
      elements = elements.map(_.asInstanceOf[Elem]),
      nextElementNumber = 0,
      characters = if (nonElements.isEmpty) None else {
        val result: String = nonElements.map(_.text).mkString.trim
        if (result.isEmpty) None else Some(result)
      }
    )
  }
}
package org.opentorah.texts.tanach

import org.opentorah.xml.{Attribute, Parser}

final case class VerseParsed(chapter: Option[Int], verse: Option[Int]) {

  def inheritFrom(ancestor: VerseParsed): VerseParsed = {
    require(this.chapter.isEmpty || ancestor.chapter.isEmpty)
    require(this.verse.isEmpty || ancestor.verse.isEmpty)

    VerseParsed(
      chapter = this.chapter.orElse(ancestor.chapter),
      verse = this.verse.orElse(ancestor.verse),
    )
  }

  def defaultChapter(defaultChapter: Int): VerseParsed =
    if (chapter.isDefined) this
    else VerseParsed(chapter = Some(defaultChapter), verse = verse)

  def resolve: Verse = Verse(chapter.get, verse.getOrElse(1))
}

object VerseParsed {

  val fromParser: Parser[VerseParsed] = for {
    chapter <- new Attribute.PositiveIntAttribute("fromChapter").optional()
    verse <- new Attribute.PositiveIntAttribute("fromVerse").optional()
  } yield VerseParsed(chapter, verse)

  val toParser: Parser[VerseParsed] = for {
    chapter <- new Attribute.PositiveIntAttribute("toChapter").optional()
    verse <- new Attribute.PositiveIntAttribute("toVerse").optional()
  } yield VerseParsed(chapter, verse)
}

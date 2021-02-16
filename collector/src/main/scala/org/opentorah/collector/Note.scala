package org.opentorah.collector

import org.opentorah.markdown.Markdown
import org.opentorah.xml.{Attribute, Element, Parsable, Parser, Unparser, Xml}

final class Note(
  override val name: String,
  val title: Option[String]
) extends Directory.Entry(name) with HtmlContent {

  override def htmlHeadTitle: Option[String] = title
  override def htmlBodyTitle: Option[Xml.Nodes] = htmlHeadTitle.map(Xml.mkText)
  override def path(site: Site): Store.Path = Seq(site.notes, this)
  override def content(site: Site): Xml.Element = site.notes.getFile(this).content
}

object Note extends Element[Note]("note") with Directory.EntryMaker[Markdown, Note] {

  override def apply(name: String, markdown: Markdown): Note = new Note(
    name,
    markdown.title
  )

  private val titleAttribute: Attribute.Optional[String] = Attribute("title").optional

  override def contentParsable: Parsable[Note] = new Parsable[Note] {
    override def parser: Parser[Note] = for {
      name <- Directory.fileNameAttribute()
      title <- titleAttribute()
    } yield new Note(
      name,
      title
    )

    override def unparser: Unparser[Note] = Unparser.concat(
      Directory.fileNameAttribute(_.name),
      titleAttribute(_.title)
    )
  }
}

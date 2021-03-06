package org.opentorah.site

import org.opentorah.docbook.DocBook
import org.opentorah.xml.{Parser, Resolver, Xml}
import zio.ZIO
import java.io.File

// TODO dissolve into Site: introduce [Pre]Content and subsume this and Markdown into it.
final class DocBookHtmlContent[S <: Site[S]](
  inputFile: File,
  resolver: Resolver
) extends org.opentorah.site.HtmlContent[S] {

  override def htmlHeadTitle: Option[String] = None // TODO

  // TODO Caching.Parser?
  override def content(site: S): Parser[Xml.Element] =
    ZIO.succeed(DocBook.loadFromFile(inputFile, Some(resolver))) // TODO real Parser
}

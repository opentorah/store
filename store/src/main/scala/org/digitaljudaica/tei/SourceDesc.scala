package org.digitaljudaica.tei

import org.digitaljudaica.xml.RawXml
import scala.xml.Node

final case class SourceDesc(xml: Seq[Node]) extends RawXml(xml)

object SourceDesc extends RawXml.Descriptor[SourceDesc]("sourceDesc", new SourceDesc(_))

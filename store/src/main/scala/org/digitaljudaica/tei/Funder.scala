package org.digitaljudaica.tei

import org.digitaljudaica.xml.RawXml
import scala.xml.Node

final case class Funder(xml: Seq[Node]) extends RawXml(xml)

object Funder extends RawXml.Descriptor[Funder]("funder", new Funder(_))

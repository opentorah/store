package org.opentorah.xml

import scala.xml.{Elem, Node}

class RawXml(elementName: String) {

  final class Value(val xml: Seq[Node])

  object parsable extends Element[Value](
    elementName,
    ContentType.Mixed,
    Element.allNodes.map(new Value(_))
  ) with ToXml[Value] {

    override def toString: String = s"raw element $elementName"

    override def toXml(value: Value): Elem = <elem>{value.xml}</elem>.copy(label = elementName)
  }
}
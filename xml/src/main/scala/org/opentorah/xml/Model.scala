package org.opentorah.xml

// This abstracts over the XML model, allowing processing of not just Scala XML.
// Support for pretty-printing DOM is planned; support for DOM in Parser is not (yet?) ;)
trait Model[N] {
  type Element <: N
  type Text <: N

  def isAtom(node: N): Boolean
  def isWhitespace(node: N): Boolean
  def isCharacters(node: N): Boolean
  def isText(node: N): Boolean
  def asText(node: N): Text
  def getText(text: Text): String
  def mkText(text: String): Text
  def getNodeText(node: N): String
  def isElement(node: N): Boolean
  def asElement(node: N): Element
  def getNamespaces(element: Element): Seq[Namespace]
  def getName(element: Element): String
  def getPrefix(element: Element): Option[String]
  def getAttributes(element: Element): Seq[Attribute.Value[String]]
  def getChildren(element: Element): Seq[N]
}

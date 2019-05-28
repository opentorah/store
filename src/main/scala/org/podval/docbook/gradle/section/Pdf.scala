package org.podval.docbook.gradle.section

import java.io.File

import org.podval.docbook.gradle.fop.{Fop, FopPlugin}
import org.podval.docbook.gradle.mathjax.{MathJax, MathReader}
import org.podval.docbook.gradle.{Layout, Logger, mathjax, jeuclid}
import org.xml.sax.XMLFilter

object Pdf extends DocBook2 {
  override def name: String = "pdf"
  override def stylesheetUriName: String = "fo/docbook"
  override def outputFileExtension: String = "pdf"
  override def usesRootFile: Boolean = true
  override def usesIntermediate: Boolean = true
  override def intermediateDirectoryName: String = "fo"
  override def intermediateFileExtension: String = "fo"
  override def additionalSections: List[Section] = List( Common)

  override def defaultParameters: Map[String, String] = Map(
    // Paper size; double-sidedness; not a draft
    "paper.type" -> "USletter",
    "double.sided" -> "yes",
    "draft.mode" -> "no",

    // FOP extensions
    "fop.extensions" -> "0",
    "fop1.extensions" -> "1"
  )

  override def customStylesheet: String =
    s"""
       |  <!-- Break before each section -->
       |  <xsl:attribute-set name="section.title.level1.properties">
       |    <xsl:attribute name="break-before">page</xsl:attribute>
       |  </xsl:attribute-set>
       |"""

  override def xmlFilter(mathJaxConfiguration: MathJax.Configuration): Option[XMLFilter] =
    Some(new MathReader(mathJaxConfiguration))

  override def postProcess(
    layout: Layout,
    substitutions: Map[String, String],
    isMathJaxEnabled: Boolean,
    isJEuclidEnabled: Boolean,
    mathJaxConfiguration: MathJax.Configuration,
    inputDirectory: File,
    inputFile: File,
    outputFile: File,
    logger: Logger
  ): Unit = {
    require(!isMathJaxEnabled || !isJEuclidEnabled)

    val plugin: Option[FopPlugin] =
      if (isJEuclidEnabled) Some(new jeuclid.FopPlugin)
      else if (isMathJaxEnabled) Some(new mathjax.FopPlugin(new MathJax(layout.nodeModulesRoot, mathJaxConfiguration)))
      else None

    Fop.run(
      configurationFile = layout.fopConfigurationFile,
      substitutions: Map[String, String],
      plugin = plugin,
      inputFile = inputFile,
      inputDirectory = inputDirectory,
      outputFile = outputFile,
      logger = logger
    )
  }
}

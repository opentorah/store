package org.podval.docbook.gradle

import org.gradle.api.artifacts.{Configuration, Dependency}
import org.gradle.api.DefaultTask
import org.gradle.api.file.{CopySpec, FileCopyDetails, RelativePath}
import org.gradle.api.provider.{ListProperty, MapProperty, Property}
import org.gradle.api.tasks.{Input, TaskAction}
import java.io.File
import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import Util.writeInto

class PrepareDocBookTask extends DefaultTask  {

  private val layout: Layout = Layout.forProject(getProject)

  private val logger: Logger = new Logger.PluginLogger(getLogger)

  @BeanProperty val xslt1version: Property[String] =
    getProject.getObjects.property(classOf[String])

  @BeanProperty val xslt2version: Property[String] =
    getProject.getObjects.property(classOf[String])

  @Input @BeanProperty val inputFileName: Property[String] =
    getProject.getObjects.property(classOf[String])

  @BeanProperty val parameters: MapProperty[String, java.util.Map[String, String]] =
    getProject.getObjects.mapProperty(classOf[String], classOf[java.util.Map[String, String]])

  @Input @BeanProperty val substitutions: MapProperty[String, String] =
    getProject.getObjects.mapProperty(classOf[String], classOf[String])

  @Input @BeanProperty val cssFileName: Property[String] =
    getProject.getObjects.property(classOf[String])

  @Input @BeanProperty val epubEmbeddedFonts: ListProperty[String] =
    getProject.getObjects.listProperty(classOf[String])

  @TaskAction
  def prepareDocBook(): Unit = {
    // Verify parameter sections
    val allParameters: Map[String, Map[String, String]] =
      parameters.get.asScala.toMap.mapValues(_.asScala.toMap)

    val sectionsPresent: Set[String] = allParameters.keySet
    val sectionsClaimed: Set[String] = DocBook2.processors.toSet[DocBook2].flatMap(_.parameterSections)
    val sectionsUnclaimed: Set[String] = sectionsPresent -- sectionsClaimed
    if (sectionsUnclaimed.nonEmpty) {
      val sections: String = DocBook2.processors.map { processor =>
        "  " + processor.name + ": " + processor.parameterSections.mkString(", ")
      }.mkString("\n")

      throw new IllegalArgumentException(
        s"""Unsupported parameter sections: ${sectionsUnclaimed.mkString(", ")}.
           |Supported sections are:
           |$sections
           |""".stripMargin
      )
    }

    // Input file
    writeInto(layout.inputFile(inputFileName.get), logger, replace = false) {
      """|<?xml version="1.0" encoding="UTF-8"?>
         |<!DOCTYPE article
         |  PUBLIC "-//OASIS//DTD DocBook XML V5.0//EN"
         |  "http://www.oasis-open.org/docbook/xml/5.0/dtd/docbook.dtd">
         |
         |<article xmlns="http://docbook.org/ns/docbook" version="5.0"
         |         xmlns:xi="http://www.w3.org/2001/XInclude">
         |</article>
         |"""
    }

    // XSLT stylesheets
    unpackDocBookXsl(Stylesheets.xslt1, xslt1version.get)
    unpackDocBookXsl(Stylesheets.xslt2, xslt2version.get)

    writeInto(layout.cssFile(cssFileName.get), logger, replace = false) {
      """@namespace xml "http://www.w3.org/XML/1998/namespace";
        |"""
    }

    // FOP configuration
    writeInto(layout.fopConfigurationFile, logger, replace = false) {
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<fop version="1.0">
         |  <renderers>
         |    <renderer mime="application/pdf">
         |      <fonts>
         |        <!-- FOP will detect fonts available in the operating system. -->
         |        <auto-detect/>
         |      </fonts>
         |    </renderer>
         |  </renderers>
         |</fop>
         |"""
    }

    // Stylesheet files and customizations

    val epubEmbeddedFontsStr: String =
      Fop.getFontFiles(layout.fopConfigurationFile, epubEmbeddedFonts.get.asScala.toList, logger)

    DocBook2.processors.foreach { processor: DocBook2 =>
      processor.writeStylesheetFiles(
        layout = layout,
        inputFileName = inputFileName.get,
        parameters = processor.parameterSections.flatMap(allParameters.get).flatten.toMap,
        cssFileName = cssFileName.get,
        epubEmbeddedFonts = epubEmbeddedFontsStr,
        logger = logger
      )
    }

    // substitutions DTD
    writeInto(layout.xmlFile(layout.substitutionsDtdFileName), logger, replace = true) {
      substitutions.get.asScala.toSeq.map {
        case (name: String, value: String) => s"""<!ENTITY $name "$value">\n"""
      }.mkString
    }

    // XML catalog
    writeInto(layout.catalogFile, logger, replace = true) {
      val data: String = layout.dataDirectoryRelative

      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<!DOCTYPE catalog
         |  PUBLIC "-//OASIS//DTD XML Catalogs V1.1//EN"
         |  "http://www.oasis-open.org/committees/entity/release/1.1/catalog.dtd">
         |
         |<!-- DO NOT EDIT! Generated by the DocBook plugin.
         |     Customizations go into ${layout.catalogCustomFileName}. -->
         |<catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog" prefer="public">
         |  <group xml:base="${layout.catalogGroupBase}">
         |    <!--
         |      There seems to be some confusion with the rewriteURI form:
         |      Catalog DTD requires 'uriIdStartString' attribute (and that is what IntelliJ wants),
         |      but XMLResolver looks for the 'uriStartString' attribute (and this seems to work in Oxygen).
         |    -->
         |
         |    <!-- DocBook XSLT 1.0 stylesheets  -->
         |    <rewriteURI uriStartString="http://docbook.sourceforge.net/release/xsl-ns/current/"
         |                rewritePrefix="${layout.docBookXslDirectoryRelative(Stylesheets.xslt1.directoryName)}"/>
         |
         |    <!-- DocBook XSLT 2.0 stylesheets  -->
         |    <rewriteURI uriStartString="https://cdn.docbook.org/release/latest/xslt/"
         |                rewritePrefix="${layout.docBookXslDirectoryRelative(Stylesheets.xslt2.directoryName)}"/>
         |
         |    <!-- generated data -->
         |    <rewriteSystem rewritePrefix="$data" systemIdStartString="data:/"/>
         |    <rewriteSystem rewritePrefix="$data" systemIdStartString="data:"/>
         |    <rewriteSystem rewritePrefix="$data" systemIdStartString="urn:docbook:data:/"/>
         |    <rewriteSystem rewritePrefix="$data" systemIdStartString="urn:docbook:data:"/>
         |    <rewriteSystem rewritePrefix="$data" systemIdStartString="urn:docbook:data/"/>
         |    <rewriteSystem rewritePrefix="$data" systemIdStartString="http://podval.org/docbook/data/"/>
         |  </group>
         |
         |  <!-- substitutions DTD -->
         |  <public publicId="-//OASIS//DTD DocBook XML V5.0//EN" uri="${layout.substitutionsDtdFileName}"/>
         |
         |  <nextCatalog catalog="${layout.catalogCustomFileName}"/>
         |</catalog>
         |"""
    }

    // XML catalog customization
    writeInto(layout.xmlFile(layout.catalogCustomFileName), logger, replace = false) {
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<!DOCTYPE catalog
         |  PUBLIC "-//OASIS//DTD XML Catalogs V1.1//EN"
         |  "http://www.oasis-open.org/committees/entity/release/1.1/catalog.dtd">
         |
         |<!-- Customizations go here. -->
         |<catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog" prefer="public">
         |  <nextCatalog catalog="/etc/xml/catalog"/>
         |</catalog>
         |"""
    }
  }

  private def unpackDocBookXsl(stylesheets: Stylesheets, version: String): Unit = {
    val directory: File = layout.docBookXslDirectory(stylesheets.directoryName)
    if (!directory.exists) {
      val dependencyNotation: String = stylesheets.dependencyNotation(version)
      logger.info(s"Retrieving DocBook ${stylesheets.name} stylesheets: $dependencyNotation")
      val file = getArtifact(stylesheets, dependencyNotation)
      logger.info(s"Unpacking ${file.getName}")
      unpack(
        zipFile = file,
        archiveSubdirectoryName = stylesheets.archiveSubdirectoryName,
        directory = directory
      )
    }
  }

  private def getArtifact(stylesheets: Stylesheets, dependencyNotation: String): File = {
    val dependency: Dependency = getProject.getDependencies.create(dependencyNotation)
    val configuration: Configuration = getProject.getConfigurations.detachedConfiguration(dependency)
    configuration.setTransitive(false)
    configuration.getSingleFile
  }

  private def unpack(zipFile: File, archiveSubdirectoryName: String, directory: File): Unit = {
    val toDrop: Int = archiveSubdirectoryName.count(_ == '/') + 1
    getProject.copy((copySpec: CopySpec) => copySpec
      .into(directory)
      .from(getProject.zipTree(zipFile))
      // following code deals with extracting just the "docbook" directory;
      // this should become easier in Gradle 5.3, see:
      // https://github.com/gradle/gradle/issues/1108
      // https://github.com/gradle/gradle/pull/5405
      .include(archiveSubdirectoryName + "/**")
      .eachFile((file: FileCopyDetails) =>
        file.setRelativePath(new RelativePath(true, file.getRelativePath.getSegments.drop(toDrop): _*))
      )
      .setIncludeEmptyDirs(false))
  }
}

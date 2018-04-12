package org.podval.docbook.gradle

import com.icl.saxon.TransformerFactoryImpl
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.{Transformer, URIResolver}
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.{StreamResult, StreamSource}
import org.apache.xerces.jaxp.SAXParserFactoryImpl
import org.gradle.api.{DefaultTask, Project}
import org.gradle.api.provider.{Property, Provider}
import org.gradle.api.tasks.{Input, InputDirectory, InputFile, OutputDirectory, OutputFile, TaskAction}
import org.xml.sax.{EntityResolver, InputSource, XMLReader}
import java.io.{File, FileReader}
import java.net.URL

object SaxonTask {
  def apply(
    project: Project,
    name: String,
    description: String,
    outputType: String,
    inputFileName: Provider[String],
    stylesheetName: String,
    dataDirectory: Property[File],
    outputFileNameOverride: Option[String] = None
  ): SaxonTask = project.getTasks.create(name, classOf[SaxonTask], (task: SaxonTask) => {
    task.setDescription(description)
    task.outputType.set(outputType)
    task.inputFileName.set(inputFileName)
    task.stylesheetName.set(stylesheetName)
    task.dataDirectory.set(dataDirectory)
    task.outputFileNameOverride.set(outputFileNameOverride)
  })
}

class SaxonTask extends DefaultTask {
  @InputDirectory
  val inputDirectory: File = DocBookPlugin.docBookDir(getProject)

  @Input
  val inputFileName: Property[String] = getProject.getObjects.property(classOf[String])

  @InputFile
  val inputFile: Provider[File] = inputFileName.map(DocBookPlugin.file(inputDirectory, _, "xml"))

  @InputDirectory
  val xslDir: File = DocBookPlugin.xslDir(getProject)

  @Input
  val stylesheetName: Property[String] = getProject.getObjects.property(classOf[String])

  @InputFile
  val stylesheetFile: Provider[File] = stylesheetName.map(DocBookPlugin.xslFile(getProject, _))

  @InputFile
  val dataDirectory: Property[File] = getProject.getObjects.property(classOf[File])

  @Input
  val outputType: Property[String] = getProject.getObjects.property(classOf[String])

  @OutputDirectory
  val outputDirectory: Provider[File] = outputType.map(DocBookPlugin.outputDirectory(getProject, _))
  def getOutputDirectory: Provider[File] = outputDirectory

  @Input
  val outputFileNameOverride: Property[Option[String]] = getProject.getObjects.property(classOf[Option[String]])

  @Input
  val outputFileName: Provider[String] = outputFileNameOverride.map(_.getOrElse(inputFileName.get))

  @OutputFile
  val outputFile: Provider[File] = outputFileName.map(DocBookPlugin.file(outputDirectory.get, _, outputType.get))

  @TaskAction
  def saxon(): Unit = {
    val input: File = inputFile.get
    val stylesheet: File = stylesheetFile.get
    val output: File = outputFile.get

    outputDirectory.get.mkdirs

    val saxParserFactory: SAXParserFactory = new SAXParserFactoryImpl
    saxParserFactory.setXIncludeAware(true)
    val xmlReader: XMLReader = saxParserFactory.newSAXParser.getXMLReader
    xmlReader.setEntityResolver(mkEntityResolver(getProject, dataDirectory.get))

    val stylesheetUrl: URL = stylesheet.toURI.toURL
    val transformer: Transformer = new TransformerFactoryImpl().newTransformer(
      new StreamSource(stylesheetUrl.openStream, stylesheetUrl.toExternalForm)
    )

    transformer.setURIResolver(mkUriResolver(getProject))

    // TODO take parameters and set them here - with defaults
    transformer.setParameter("root.filename", outputFileName.get)
    transformer.setParameter("base.dir", outputDirectory.get + File.separator)

    transformer.transform(
      new SAXSource(
        xmlReader,
        new InputSource(input.getAbsolutePath)
      ),
      new StreamResult(output.getAbsolutePath)
    )
  }

  // Resolves references to data in DocBook files
  def mkEntityResolver(project: Project, dataDirectory: File): EntityResolver = (publicId: String, systemId: String) => {
    drop("http://podval.org/docbook/data/", systemId).map { path =>
      val result = new InputSource(new FileReader(new File(dataDirectory, path)))
      result.setSystemId(systemId)
      result
    }.orNull
  }

  // Resolves references to DocBook XSL in customization files
  def mkUriResolver(project: Project): URIResolver = (href: String, base: String) => {
    drop("http://podval.org/docbook/data/", href).orElse(drop("http://docbook.sourceforge.net/release/xsl-ns/current/", href)
    ).map(path => new StreamSource(new File(DocBookPlugin.docBookXsl(project), path))).orNull
  }

  private def drop(what: String, from: String): Option[String] =
    if (from.startsWith(what)) Some(from.drop(what.length)) else None
}

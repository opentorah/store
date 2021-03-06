package org.opentorah.util

import java.io.File
import org.slf4j.{Logger, LoggerFactory}
import scala.sys.process.{Process, ProcessLogger}

object Platform {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def getOsName: String = System.getProperty("os.name")

  def getOs: Os = {
    val name: String = getOsName.toLowerCase
    val result = Os.values.find(_.name.toLowerCase.contains(name.toLowerCase))
      .getOrElse(throw new IllegalArgumentException(s"Unsupported OS: $name"))
    if (result == Os.Linux && System.getProperty("java.specification.vendor").contains("Android")) Os.Android
    else result
  }

  // Note: Gradle Node plugin's code claims that Java returns "arm" on all ARM variants.
  def getEnvironmentArchName: String = System.getProperty("os.arch")

  def getSystemArchName: String = exec(command = "uname -m")

  def getArchName: String = if (getOs.hasUname) getSystemArchName else getEnvironmentArchName

  def getArch: Architecture = {
    val name = getArchName
    Architecture.values.find(_.name.toLowerCase == name.toLowerCase)
      .getOrElse(throw new IllegalArgumentException(s"Unsupported architecture: $name"))
  }

  def which(what: String): Option[File] =
    execOption(command = s"which $what").map(new File(_))

  def exec(command: String): String = Process(command).!!.trim

  def execOption(command: String): Option[String] =
    try {
      Some(exec(command))
    } catch {
      case _: Exception => None
    }

  def exec(
    command: File,
    args: Seq[String],
    cwd: Option[File],
    extraEnv: (String, String)*
  ): String = {
    val cmd: Seq[String] = command.getAbsolutePath +: args
    logger.debug(
      s"""Platform.exec(
         |  cmd = $cmd,
         |  cwd = $cwd,
         |  extraEnv = $extraEnv
         |)""".stripMargin
    )

    var err: Seq[String] = Seq.empty
    var out: Seq[String] = Seq.empty

    val exitCode = Process(
      command = cmd,
      cwd,
      extraEnv = extraEnv: _*
    ).!(ProcessLogger(line => err = err :+ line, line => out = out :+ line))

    val errStr = err.mkString("\n")
    val outStr = out.mkString("\n")

    val result = s"Platform.exec() => exitCode=$exitCode; err=$errStr; out=$outStr"
    if (exitCode == 0) logger.debug(result) else logger.error(result)
    if (exitCode == 0) outStr else throw new IllegalArgumentException(s"Platfor.exec() => exitCode=$exitCode")
  }
}

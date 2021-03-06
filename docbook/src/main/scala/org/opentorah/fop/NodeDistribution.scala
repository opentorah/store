package org.opentorah.fop

import org.opentorah.util.{Architecture, Os, Platform}
import org.slf4j.{Logger, LoggerFactory}
import java.io.File

// Heavily inspired by (read: copied and reworked from :)) https://github.com/srs/gradle-node-plugin by srs.
// That plugin is not used directly because its tasks are not reusable unless the plugin is applied to the project,
// and I do not want to apply Node plugin to every project that uses DocBook.
// Also, I want to be able to run npm from within my code without creating tasks.
// My simplified Node support is under 200 lines.

// Describes Node distribution's packaging and structure.
final class NodeDistribution(val version: String) {
  private val logger: Logger = LoggerFactory.getLogger(classOf[NodeDistribution])

  val os: Os = Platform.getOs
  val architecture: Architecture = Platform.getArch

  override def toString: String = s"Node v$version for $os on $architecture"

  private val osName: String = os match {
    case Os.Windows => "win"
    case Os.Mac     => "darwin"
    case Os.Linux   => "linux"
    case Os.FreeBSD => "linux"
    case Os.SunOS   => "sunos"
    case Os.Aix     => "aix"
    case _          => throw new IllegalArgumentException (s"Unsupported OS: $os")
  }

  val isWindows: Boolean = os == Os.Windows

  private val osArch: String = architecture match {
    case Architecture.x86_64  => "x64"
    case Architecture.amd64   => "x64"
    case Architecture.aarch64 => "x64"
    case Architecture.ppc64   => "ppc64"
    case Architecture.ppc64le => "ppc64le"
    case Architecture.s390x   => "s390x"
    case Architecture.armv6l  => "armv6l"
    case Architecture.armv7l  => "armv7l"
    case Architecture.armv8l  => "arm64" // *not* "armv8l"!
    case Architecture.i686    => "x86"
    case Architecture.nacl    => "x86"
  }

  private val versionTokens: Array[String] = version.split('.')
  private val majorVersion: Int = versionTokens(0).toInt
  private val minorVersion: Int = versionTokens(1).toInt
  private val microVersion: Int = versionTokens(2).toInt

  //https://github.com/nodejs/node/pull/5995
  private def hasWindowsZip: Boolean =
    ((majorVersion == 4) && (minorVersion >= 5)) || // >= 4.5.0..6
    ((majorVersion == 6) && ((minorVersion > 2) || ((minorVersion == 2) && (microVersion >= 1)))) || // >= 6.2.1..7
     (majorVersion >  6) // 7..

  private def fixUpOsAndArch: Boolean = isWindows && !hasWindowsZip
  private val dependencyOsName: String = if (fixUpOsAndArch) "linux" else osName
  private val dependencyOsArch: String = if (fixUpOsAndArch) "x86" else osArch

  def isZip: Boolean = isWindows && hasWindowsZip
  private val ext: String = if (isZip) "zip" else "tar.gz"

  val dependencyNotation: String =
    s"org.nodejs:node:$version:$dependencyOsName-$dependencyOsArch@$ext"

  def getRoot(into: File): File = new File(into, topDirectory)

  val topDirectory: String =
    s"node-v$version-$dependencyOsName-$dependencyOsArch"

  def getBin(root: File): File = if (hasBinSubdirectory) new File(root, "bin") else root

  def hasBinSubdirectory: Boolean = !isWindows
}

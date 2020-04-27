package org.podval.fop.mathjax

import java.lang.reflect.Field

import org.podval.fop.util.Util.mapValues
import com.eclipsesource.v8.V8
import org.podval.fop.util.Logger

// TODO for Scala 2.13: import scala.jdk.CollectionConverters._
import scala.collection.JavaConverters._

final class J2V8(libraryPath: String) {

  override def toString: String = s"J2V8 library $libraryPath"

  def load(logger: Logger): Boolean = {
    try {
      System.load(libraryPath)

      logger.info(s"Loaded $this")
      val field: Field = classOf[V8].getDeclaredField("nativeLibraryLoaded")
      field.setAccessible(true)
      field.set(null, true)
      true
    } catch {
      case e: UnsatisfiedLinkError =>
        logger.warn(s"Failed to load $this: ${e.getMessage}")
        false
    }
  }
}

object J2V8 {

  def map2java(map: Map[String, Any]): java.util.Map[String, Any] =
    mapValues(map)(value2java).asJava

  def list2java(list: List[Any]): java.util.List[Any] =
    list.map(value2java).asJava

  private def value2java(value: Any): Any = value match {
    // with value: Map[String, Any] I get:
    //   non-variable type argument String in type pattern scala.collection.immutable.Map[String,Any]
    //   (the underlying of Map[String,Any]) is unchecked since it is eliminated by erasure
    case value: Map[_, Any] => map2java(value.asInstanceOf[Map[String, Any]])
    case value: List[Any] => list2java(value)
    case other => other
  }
}

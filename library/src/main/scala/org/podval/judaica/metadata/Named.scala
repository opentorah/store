package org.podval.judaica.metadata

import org.podval.judaica.util.Util

trait Named extends WithName with WithNames {
  override def name: String = Util.className(this)

  override def toString: String = name
}

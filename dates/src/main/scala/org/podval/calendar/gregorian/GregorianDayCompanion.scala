package org.podval.calendar.gregorian

import org.podval.calendar.dates.{Calendar, DayCompanion}
import org.podval.judaica.metadata.{Named, Names}

abstract class GregorianDayCompanion extends DayCompanion[Gregorian] {
  final override val Name: GregorianDayCompanion.type = GregorianDayCompanion

  // TODO push into DayCompanion?
  final override def names: Seq[Name] = GregorianDayCompanion.values

  final override val firstDayNumberInWeek: Int = Calendar.firstDayNumberInWeekGregorian
}

object GregorianDayCompanion extends Named {
  sealed trait Key extends Named.NamedBase {
    final override def names: Names = toNames(this)
  }

  case object Sunday extends Key
  case object Monday extends Key
  case object Tuesday extends Key
  case object Wednesday extends Key
  case object Thursday extends Key
  case object Friday extends Key
  case object Saturday extends Key

  override val values: Seq[Key] = Seq(Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday)

  protected override def resourceName: String = "GregorianDay"
}

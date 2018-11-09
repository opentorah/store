package org.podval.calendar.jewish

import org.podval.calendar.dates.DayBase

abstract class JewishDay(number: Int) extends DayBase[Jewish](number) {
  final def isShabbos: Boolean = is(Jewish.Day.Name.Shabbos)

  final def isRoshChodesh: Boolean = (numberInMonth == 1) || (numberInMonth == 30)

  final def shabbosAfter: Jewish.Day = next.next(Jewish.Day.Name.Shabbos)

  final def shabbosBefore: Jewish.Day = prev.prev(Jewish.Day.Name.Shabbos)
}

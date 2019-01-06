package org.podval.calendar.service

import org.podval.calendar.dates.{Calendar, DayBase, MonthBase, YearBase}
import org.podval.calendar.gregorian.Gregorian
import org.podval.calendar.jewish.{Cycle, Jewish, SeasonsFixed, Shemittah, SunCycle, YearType}
import org.podval.calendar.rambam.{RambamSchedule, SeferHamitzvosLessons}
import org.podval.calendar.tanach.{Chitas, Haftarah, Reading, Schedule, SpecialDay}
import org.podval.judaica.metadata.{Language, LanguageSpec, WithNames}
import org.podval.judaica.rambam.MishnehTorah
import org.podval.judaica.tanach.{Custom, Span, Torah}
import org.podval.judaica.tanach.Tanach.Psalms
import org.podval.judaica.util.Util
import scalatags.Text.TypedTag
import scalatags.Text.all._

sealed abstract class Renderer(location: Location, spec: LanguageSpec) {
  protected type C <: Calendar[C]

  protected val calendar: C

  private final def getName(other: Boolean = false): String = if (!other) name else otherName

  protected def name: String

  protected def otherName: String

  private final def now: C#Day = calendar.Day.now

  private final def getYear(yearStr: String): C#Year = calendar.Year(yearStr.toInt)

  private final def getMonth(yearStr: String, monthStr: String): C#Month = {
    val year = getYear(yearStr)
    val monthName: Option[C#MonthName] = calendar.Month.Name.forName(monthStr)
    if (monthName.isDefined) year.month(monthName.get)
    else year.month(monthStr.toInt)
  }

  protected def jewish(day: DayBase[_]): Jewish.Day

  protected def gregorian(day: DayBase[_]): Gregorian.Day

  protected def first(day: DayBase[_]): DayBase[_]

  protected def second(day: DayBase[_]): DayBase[_]

  //  private def toLanguageString(what: LanguageString): String = what.toLanguageString(spec)

  private def yearUrl(year: YearBase[_], other: Boolean = false): String =
    s"/${getName(other)}/${year.number}"

  private def monthUrl(month: MonthBase[_], other: Boolean = false): String =
    s"/${getName(other)}/${month.year.number}/${month.numberInYear}"

  private def monthNameUrl(month: MonthBase[_], other: Boolean = false): String =
    s"/${getName(other)}/${month.year.number}/${month.name.toLanguageString(spec)}"

  private def dayUrl(day: DayBase[_], other: Boolean = false): String =
    s"/${getName(other)}/${day.year.number}/${day.month.numberInYear}/${day.numberInMonth}"


  private def yearLink(year: YearBase[_], other: Boolean = false, text: Option[String] = None): TypedTag[String] =
    navLink(yearUrl(year, other = other), text.getOrElse(year.toLanguageString(spec)))

  private def monthLink(month: MonthBase[_]): TypedTag[String] =
    navLink(monthUrl(month), month.numberInYearToLanguageString(spec))

  private def monthNameLink(month: MonthBase[_], other: Boolean = false, text: Option[String] = None): TypedTag[String] =
    navLink(monthNameUrl(month, other = other), text.getOrElse(month.name.toLanguageString(spec)))

  private def dayLink(day: DayBase[_], other: Boolean = false, text: Option[String] = None): TypedTag[String] =
    navLink(dayUrl(day, other = other), text.getOrElse(day.numberInMonthToLanguageString(spec)))

  private def navLink(url: String, text: String): TypedTag[String] =
    a(cls := "nav", href := s"$url$suffix")(text)

  private def suffix: String = Renderer.suffix(location, spec)

  private def dayLinks(day: DayBase[_], other: Boolean): TypedTag[String] = span(
    yearLink(day.year, other = other),
    monthNameLink(day.month, other = other),
    dayLink(day-1, other = other, text = Some("<")),
    dayLink(day, other = other),
    dayLink(day+1, other = other, text = Some(">"))
  )

  def renderLanding: String = {
    val day: DayBase[_] = now
    renderHtml(s"/$name", dayLinks(day, other = false))
  }

  def renderYear(yearStr: String): String = {
    val year: YearBase[_] = getYear(yearStr)
    renderHtml(
      yearUrl(year),
      div(
        yearLink(year-1, text = Some("<")),
        yearLink(year),
        yearLink(year+1, text = Some(">")),
        table(tbody(year.months.map { month: MonthBase[_] =>
          tr(
            td(monthLink(month)),
            td(monthNameLink(month))
          )
        })),
        renderYearInformation(year)
      )
    )
  }

  protected def renderYearInformation(year: YearBase[_]): Seq[TypedTag[String]] = Seq.empty

  def renderMonth(yearStr: String, monthStr: String): String = {
    val month: MonthBase[_] = getMonth(yearStr, monthStr)
    renderHtml(monthNameUrl(month), div(
      yearLink(month.year),
      monthNameLink(month-1, text = Some("<")),
      monthNameLink(month),
      monthNameLink( month+1, text = Some(">")),
      table(tbody(month.days.map { day: DayBase[_] => tr(td(dayLink(day))) }))
    ))
  }

  def renderDay(yearStr: String, monthStr: String, dayStr: String): String = {
    val day: DayBase[_] = getMonth(yearStr, monthStr).day(dayStr.toInt)
    val jewishDay: Jewish.Day = jewish(day)
    val firstDay: DayBase[_] = first(day)
    val secondDay: DayBase[_] = second(day)

    val daySchedule = Schedule.get(jewishDay, inHolyLand = location.inHolyLand)

    renderHtml(dayUrl(firstDay), div(
      div(dayLinks(firstDay, other = false), " ", firstDay.name.toLanguageString(spec)),
      div(dayLinks(secondDay, other = true), " ", secondDay.name.toLanguageString(spec)),
      div(daySchedule.dayNames.map { withNames: WithNames => span(cls := "name", withNames.names.doFind(spec).name) }),
      renderOptionalReading("Morning", daySchedule.morning),
      renderOptionalReading("Purim morning alternative", daySchedule.purimAlternativeMorning),
      if (!jewishDay.isShabbosMevarchim) Seq.empty[TypedTag[String]] else renderShabbosMevarchim(jewishDay.month.next),
      span(cls := "heading")("Chitas"),
      renderChitas(daySchedule.chitas),
      span(cls := "heading")("Tehillim"),
      renderTehillim(
        if ((jewishDay.numberInMonth == 29) && jewishDay.month.length == 29) Span(Psalms.days(29-1).from, Psalms.days(30-1).to)
        else Psalms.days(jewishDay.numberInMonth)
      ),
      div(cls := "heading")("Rambam"),
      renderRambam(RambamSchedule.forDay(jewishDay)),
      renderOptionalReading("Afternoon", daySchedule.afternoon)
    ))
  }

  private def renderShabbosMevarchim(month: Jewish.Month): Seq[TypedTag[String]] = Seq(
    span(cls := "subheading")("Shabbos Mevarchim"),
    table(tr(
      td(month.name.toLanguageString(spec)),
      if (month.prev.length == 30) td(month.prev.lastDay.name.toLanguageString(spec)) else td(),
      td(month.firstDay.name.toLanguageString(spec)),
      td(month.newMoon.toLanguageString(spec))
    ))
  )

  private def renderChitas(chitas: Chitas): TypedTag[String] = {
    def renderFragment(fragment: Torah.Fragment) =
      Seq(td(span(fragment.toLanguageString(spec))), td(renderSource(fragment.source)))

    table(tbody(
      tr(renderFragment(chitas.first)) +:
        chitas.second.fold(Seq.empty[TypedTag[String]])(fragment => Seq(tr(renderFragment(fragment))))
    ))
  }

  private def renderTehillim(what: Span): TypedTag[String] = {
    span(what.toLanguageString(spec))
  }

  private def renderRambam(schedule: RambamSchedule): Seq[TypedTag[String]] = Seq(
    span(cls := "subheading")("3 chapters"),
    table(
      tr(td("Cycle"), td("Lesson")),
      tr(
        td(spec.toString(schedule.threeChapters.cycle)),
        td(spec.toString(schedule.threeChapters.lesson))
      )
    ),
    span(cls := "subheading")("chapters"),
    table(
      tr(td(), td("Book"), td("Part"), td("Chapter")),
      tr(td(spec.toString(1)), renderRambamChapter(schedule.threeChapters.chapter1)),
      tr(td(spec.toString(2)), renderRambamChapter(schedule.threeChapters.chapter2)),
      tr(td(spec.toString(3)), renderRambamChapter(schedule.threeChapters.chapter3))
    ),
    span(cls := "subheading")("Sefer Hamitzvos"),
    table(schedule.seferHamitzvos.parts.map { part: SeferHamitzvosLessons.Part =>
      tr(td(part.toLanguageString(spec)))
    }),
    span(cls := "subheading")("1 chapter"),
    table(
      tr(td("Cycle"), td("Year"), td("Chapter number")),
      tr(
        td(spec.toString(schedule.oneChapter.cycle)),
        td(spec.toString(schedule.oneChapter.year)),
        td(spec.toString(schedule.oneChapter.chapterNumber))
      )
    ),
    span(cls := "subheading")("chapter"),
    table(
      tr(td("Book"), td("Part"), td("Chapter")),
      tr(renderRambamChapter(schedule.oneChapter.chapter))
    )
  )

  private def renderRambamChapter(chapter: MishnehTorah.Chapter): Seq[TypedTag[String]] = Seq(
    td(chapter.part.book.toLanguageString(spec)),
    td(chapter.part.toLanguageString(spec)),
    td(chapter.toLanguageString(spec)),
  )

  private def renderOptionalReading(name: String, reading: Option[Reading]): Seq[TypedTag[String]] = {
    reading.fold(Seq.empty[TypedTag[String]]) { reading => Seq(div(
      span(cls := "heading")(name),
      {
        val maftirCommonOnly = reading.maftir.commonOnly
        val haftarahCommonOnly = reading.haftarah.commonOnly
        val noMaftirHaftarah: Boolean =
          maftirCommonOnly.fold(false)(_.isEmpty) && haftarahCommonOnly.fold(false)(_.isEmpty)
        val varyingMaftirAndHaftarah: Boolean = maftirCommonOnly.isEmpty && haftarahCommonOnly.isEmpty

        div(
          renderCustoms("Torah", reading.torah, renderTorah),
          if (noMaftirHaftarah) Seq.empty[TypedTag[String]] else
          if (varyingMaftirAndHaftarah)
            renderCustoms("Maftir and Haftarah", reading.maftirAndHaftarah, renderMaftirAndHaftarah)
          else
            renderCustoms("Maftir", reading.maftir, renderMaftir) ++
              renderCustoms("Haftarah", reading.haftarah, renderHaftarah)
        )
      }
    ))}
  }

  private def renderTorah(torah: Torah): Seq[TypedTag[String]] =
    torah.spans.zipWithIndex map { case (fragment, index) => tr(
      td(spec.toString(index + 1)),
      td(fragment.toLanguageString(spec)),
      td(renderSource(fragment.source))
    )}

  private def renderMaftir(maftir: Option[Torah.Maftir]): Seq[TypedTag[String]] =
    Seq(maftir.fold(tr(td("None")))(maftir => tr(
      td(maftir.toLanguageString(spec)),
      td(renderSource(maftir.source)))
    ))

  private def renderHaftarah(haftarah: Option[Haftarah]): Seq[TypedTag[String]] =
    haftarah.fold(Seq(tr(td("None")))){ haftarah =>
      val spans = haftarah.spans
      val parts: Seq[Seq[Haftarah.BookSpan]] =
        Util.group[Haftarah.BookSpan, Option[WithNames]](spans, span => span.source)
      parts map { part: Seq[Haftarah.BookSpan] => tr(
        td(Haftarah.toLanguageString(part)(spec)),
        td(renderSource(part.head.source))
      )}}

  private def renderMaftirAndHaftarah(maftirAndHaftarah: Option[Reading.MaftirAndHaftarah]): Seq[TypedTag[String]] =
    renderMaftir(maftirAndHaftarah.flatMap(_.maftir)) ++
      renderHaftarah(maftirAndHaftarah.map(_.haftarah))

  private def renderSource(source: Option[WithNames]): String =
    source.fold[String]("")(_.toLanguageString(spec))

  private def renderCustoms[T](
    what: String,
    customs: Custom.Of[T],
    renderer: T => Seq[TypedTag[String]]
  ): Seq[TypedTag[String]] =
    span(cls := "subheading")(what) +:
      customs.customs.toSeq.map { case (custom: Custom, value: T) =>
        table(cls := "custom")(
          caption(custom.toLanguageString(spec)),
          tbody(renderer(value))
        )
      }

  private def renderHtml(url: String, content: TypedTag[String]): String =
    Renderer.renderHtml(url, content, location, spec)
}

object Renderer {
  private val jewishRendererName: String = "jewish"

  private val gregorianRenderername: String = "gregorian"

  private final class JewishRenderer(location: Location, spec: LanguageSpec) extends Renderer(location, spec) {
    override protected type C = Jewish

    override protected val calendar: C = Jewish

    override protected def name: String = jewishRendererName

    override protected def otherName: String = gregorianRenderername

    override protected def jewish(day: DayBase[_]): Jewish.Day = day.asInstanceOf[Jewish.Day]

    override protected def gregorian(day: DayBase[_]): Gregorian.Day = Calendar.fromJewish(jewish(day))

    override protected def first(day: DayBase[_]): DayBase[_] = jewish(day)

    override protected def second(day: DayBase[_]): DayBase[_] = gregorian(day)

    override protected def renderYearInformation(yearRaw: YearBase[_]): Seq[TypedTag[String]] = {
      val year: Jewish.Year = yearRaw.asInstanceOf[Jewish.Year]

      val numbers: TypedTag[String] = table(
        tr(td("from creation"), td(year.toLanguageString(spec))),
        tr(td("is leap"), td(year.isLeap.toString())),
        tr(td("months"), td(spec.toString(year.lengthInMonths))),
        tr(td("days"), td(spec.toString(year.lengthInDays))),
        tr(td("type"), td(YearType.forYear(year).toString)),
        tr(td("in Calendar cycle"), td(spec.toString(Cycle.yearNumberInCycle(year.number)))),
        tr(td("in Shemittah cycle"), td(spec.toString(Shemittah.yearNumberInCycle(year)))),
        tr(td("in Birkchas Hachamo cycle"), td(spec.toString(SunCycle.yearNumberInCycle(year)))),
      )

      def tkufa(name: String, number: Int, year: Jewish.Year): TypedTag[String] = tr(
        td(name),
        td(SeasonsFixed.Shmuel.tkufa(year, number).toGregorianLanguageString(spec)),
        td(SeasonsFixed.RavAda.tkufa(year, number).toGregorianLanguageString(spec))
      )

      val tkufot: TypedTag[String] = table(
        tr(td("Tkufa"), td("Shmuel"), td("Rav Ada")),
        tkufa("Tishrei", 2, year-1),
        tkufa("Teves", 3, year-1),
        tkufa("Nisan", 0, year),
        tkufa("Tammuz", 1, year)
      )

      val festivalDays: Seq[(SpecialDay.Date, Jewish.Day)] =
        SpecialDay.daysWithSpecialReadings(location == Location.HolyLand)
          .map(specialDay => specialDay -> specialDay.correctedDate(year))
          .toSeq.sortBy(_._2)

      val festivals: TypedTag[String] = table(
        festivalDays.map { case (specialDay, day) => tr(
          td(specialDay.toLanguageString(spec)),
          td(day.toLanguageString(spec)),
          td(day.name.toLanguageString(spec))
        )}
      )

      Seq(
        span(cls := "heading")("Year"),
        numbers,
        span(cls := "heading")("Tkufot"),
        tkufot,
        span(cls := "heading")("Special Days"),
        festivals
      )
    }
  }

  private final class GregorianRenderer(location: Location, spec: LanguageSpec) extends Renderer(location, spec) {
    override protected type C = Gregorian

    override protected val calendar: C = Gregorian

    override protected def name: String = gregorianRenderername

    override protected def otherName: String = jewishRendererName

    override protected def jewish(day: DayBase[_]): Jewish.Day = Calendar.toJewish(gregorian(day))

    override protected def gregorian(day: DayBase[_]): Gregorian.Day = day.asInstanceOf[Gregorian.Day]

    override protected def first(day: DayBase[_]): DayBase[_] = gregorian(day)

    override protected def second(day: DayBase[_]): DayBase[_] = jewish(day)
  }

  def renderer(kindStr: String, location: Location, spec: LanguageSpec): Renderer = {
    if (kindStr == jewishRendererName) new JewishRenderer(location, spec)
    else if (kindStr == gregorianRenderername) new GregorianRenderer(location, spec)
    else throw new IllegalArgumentException(s"Unrecognized kind $kindStr")
  }

  def renderRoot(location: Location, spec: LanguageSpec): String = renderHtml(
    url = "/",
    content = div(
      div(a(href := s"/$jewishRendererName")("jewish")),
      div(a(href := s"/$gregorianRenderername")("gregorian"))
    ),
    location = location,
    spec = spec
  )

  def renderHtml(
    url: String,
    content: TypedTag[String],
    location: Location,
    spec: LanguageSpec
  ): String = {
    val languages = Language.values.map(_.toSpec) map { spec1 =>
      val languageName = spec1.languageName
      if (spec1.language == spec.language) span(cls := "picker", languageName)
      else a(cls := "picker", href := s"$url${suffix(location, spec1)}")(languageName)
    }

    val locations = Seq(Location.HolyLand, Location.Diaspora).map { location1 =>
      if (location1 == location) span(cls := "picker", location1.name)
      else a(cls := "picker", href := s"$url${suffix(location1, spec)}")(location1.name)
    }

    val result = html(dir := direction(spec))(
      head(
        //        title("Reading Schedule"),
        link(rel := "stylesheet", `type` := "text/css", href := "/style.css")
      ),
      body(
        languages,
        locations,
        content
      )
    )

    result.render
  }

  private def direction(spec: LanguageSpec): String =
    if (spec.language.contains(Language.Hebrew)) "rtl" else "ltr"

  private def suffix(location: Location, spec: LanguageSpec): String =
    s"?inHolyLand=${location.inHolyLand}&lang=${spec.languageName}"
}

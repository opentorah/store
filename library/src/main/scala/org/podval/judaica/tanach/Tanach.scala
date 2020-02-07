package org.podval.judaica.tanach

import org.digitaljudaica.metadata.{Attributes, Holder, Metadata, Named, NamedCompanion, Names, Xml}
import org.digitaljudaica.util.Collections
import org.digitaljudaica.util.Collections.mapValues
import org.podval.judaica.tanach.Torah.Maftir

import scala.xml.Elem

object Tanach extends NamedCompanion {

  override type Key = TanachBook

  sealed trait TanachBook extends Named {
    final def chapters: Chapters = toChapters(this)
  }

  override lazy val toNames: Map[TanachBook, Names] = metadatas.names

  private lazy val toChapters: Map[TanachBook, Chapters] = metadatas.chapters

  sealed abstract class ChumashBook(val parshiot: Seq[Parsha]) extends TanachBook with NamedCompanion {
    final override type Key = Parsha

    final override def values: Seq[Parsha] = parshiot

    private val metadatas: ChumashBookMetadataHolder = new ChumashBookMetadataHolder(this)

    final override def names: Names = toNames(parshiot.head)

    final override lazy val toNames: Map[Parsha, Names] = metadatas.names

    lazy val span: Map[Parsha, Span] = metadatas.span

    lazy val days: Map[Parsha, Torah.Customs] = metadatas.days

    lazy val daysCombined: Map[Parsha, Option[Torah.Customs]] = metadatas.daysCombined

    lazy val aliyot: Map[Parsha, Torah] = metadatas.aliyot

    lazy val maftir: Map[Parsha, Maftir] = metadatas.maftir
  }

  case object Genesis extends ChumashBook(Parsha.genesis)
  case object Exodus extends ChumashBook(Parsha.exodus)
  case object Leviticus extends ChumashBook(Parsha.leviticus)
  case object Numbers extends ChumashBook(Parsha.numbers)
  case object Deuteronomy extends ChumashBook(Parsha.deuteronomy)

  val chumash: Seq[ChumashBook] = Seq(Genesis, Exodus, Leviticus, Numbers, Deuteronomy)

  def getChumashForName(name: String): ChumashBook = getForName(name).asInstanceOf[ChumashBook]

  sealed trait NachBook extends TanachBook {
    final override def names: Names = toNames(this)
  }

  sealed trait ProphetsBook extends NachBook

  sealed trait EarlyProphetsBook extends ProphetsBook

  case object Joshua extends EarlyProphetsBook
  case object Judges extends EarlyProphetsBook
  case object SamuelI extends EarlyProphetsBook { override def name: String = "I Samuel" }
  case object SamuelII extends EarlyProphetsBook { override def name: String = "II Samuel" }
  case object KingsI extends EarlyProphetsBook { override def name: String = "I Kings" }
  case object KingsII extends EarlyProphetsBook { override def name: String = "II Kings" }

  val earlyProphets: Seq[ProphetsBook] = Seq(Joshua, Judges, SamuelI, SamuelII, KingsI, KingsII)

  sealed trait LateProphetsBook extends ProphetsBook

  case object Isaiah extends LateProphetsBook
  case object Jeremiah extends LateProphetsBook
  case object Ezekiel extends LateProphetsBook

  <!-- תרי עשר -->
  sealed trait TreiAsarBook extends LateProphetsBook

  case object Hosea extends TreiAsarBook
  case object Joel extends TreiAsarBook
  case object Amos extends TreiAsarBook
  case object Obadiah extends TreiAsarBook
  case object Jonah extends TreiAsarBook
  case object Micah extends TreiAsarBook
  case object Nahum extends TreiAsarBook
  case object Habakkuk extends TreiAsarBook
  case object Zephaniah extends TreiAsarBook
  case object Haggai extends TreiAsarBook
  case object Zechariah extends TreiAsarBook
  case object Malachi extends TreiAsarBook

  val treiAsar: Seq[TreiAsarBook] = Seq(Hosea, Joel, Amos, Obadiah, Jonah, Micah,
    Nahum, Habakkuk, Zephaniah, Haggai, Zechariah, Malachi)

  val lateProphets: Seq[ProphetsBook] = Seq(Isaiah, Jeremiah, Ezekiel) ++ treiAsar

  val prophets: Seq[ProphetsBook] = earlyProphets ++ lateProphets

  def getProhetForName(name: String): ProphetsBook = getForName(name).asInstanceOf[ProphetsBook]

  sealed trait WritingsBook extends NachBook

  case object Psalms extends WritingsBook {
    private def parseNumbered(element: Elem, name: String): WithNumber[SpanParsed] = {
      val attributes: Attributes = Xml.openEmpty(element, name)
      val result: WithNumber[SpanParsed] = WithNumber.parse(attributes, SpanParsed.parse)
      attributes.close()
      result
    }

    private def parseSpans(elements: Seq[Elem], name: String, number: Int): Seq[Span] = {
      val spans: Seq[SpanParsed] = WithNumber.dropNumbers(WithNumber.checkNumber(
        elements.map(element => parseNumbered(element, name)), number, name))
      SpanSemiResolved.setImpliedTo(spans.map(_.semiResolve), chapters.full, chapters)
    }

    private def allElements: (Seq[Elem], Seq[Elem], Seq[Elem]) =
      Xml.span(metadatas.psalmsElements, "day", "weekDay", "book")

    lazy val days: Seq[Span] = parseSpans(allElements._1, "day", 30)

    lazy val weekDays: Seq[Span] = parseSpans(allElements._2, "weekDay", 7)

    lazy val books: Seq[Span] = parseSpans(allElements._3, "book", 5)
  }

  case object Proverbs extends WritingsBook
  case object Job extends WritingsBook
  case object SongOfSongs extends WritingsBook { override def name: String = "Song of Songs" }
  case object Ruth extends WritingsBook
  case object Lamentations extends WritingsBook
  case object Ecclesiastes extends WritingsBook
  case object Esther extends WritingsBook
  case object Daniel extends WritingsBook
  case object Ezra extends WritingsBook
  case object Nehemiah extends WritingsBook
  case object ChroniclesI extends WritingsBook { override def name: String = "I Chronicles" }
  case object ChroniclesII extends WritingsBook { override def name: String = "II Chronicles" }

  val writings: Seq[WritingsBook] = Seq(Psalms, Proverbs, Job, SongOfSongs, Ruth, Lamentations, Ecclesiastes,
    Esther, Daniel, Ezra, Nehemiah, ChroniclesI, ChroniclesII)

  val nach: Seq[TanachBook] = prophets ++ writings

  override val values: Seq[TanachBook] = chumash ++ nach

  private final case class TanachMetadata(names: Names, chapters: Chapters, elements: Seq[Elem])

  private object metadatas extends Holder[TanachBook, TanachMetadata] {
    protected override def calculate: Map[TanachBook, TanachMetadata] = Metadata.loadMetadata(
      keys = values,
      obj = Tanach.this,
      elementName = "book"
    ).map { case (book, metadata) =>
      metadata.attributes.close()
      val (chapterElements: Seq[Elem], elements: Seq[Elem]) = Xml.take(metadata.elements, "chapter")
      if (!book.isInstanceOf[ChumashBook] && (book != Psalms)) Xml.checkNoMoreElements(elements)
      book -> TanachMetadata(metadata.names, Chapters(chapterElements), elements)
    }

    override def names: Map[TanachBook, Names] = mapValues(get)(_.names)

    def chapters: Map[TanachBook, Chapters] = mapValues(get)(_.chapters)

    def psalmsElements: Seq[Elem] = get(Psalms).elements
  }

  private final case class ParshaMetadata(
    names: Names,
    span: SpanSemiResolved,
    days: Custom.Sets[Seq[Torah.Numbered]],
    daysCombined: Custom.Sets[Seq[Torah.Numbered]],
    aliyot: Seq[Torah.Numbered],
    maftir: SpanSemiResolved
  )

  private final class ChumashBookMetadataHolder(book: ChumashBook) extends Holder[Parsha, ParshaMetadata] {
    protected override def calculate: Map[Parsha, ParshaMetadata] =
      mapValues(Metadata.bind(
        keys = book.parshiot,
        elements = Xml.span(Tanach.metadatas.get(book).elements, "week"),
        obj = this
      )){ metadata =>

        def byCustom(days: Seq[Tanach.DayParsed]): Custom.Sets[Seq[Torah.Numbered]] =
          mapValues(days.groupBy(_.custom))(days => days.map(_.span))

        val span = parseSemiResolved(metadata.attributes)
        metadata.attributes.close()

        val (aliyahElements, dayElements, maftirElements) = Xml.span(metadata.elements,
          "aliyah", "day", "maftir")
        require(maftirElements.length == 1)

        val (days: Seq[DayParsed], daysCombined: Seq[DayParsed]) = dayElements.map(parseDay).partition(!_.isCombined)

        ParshaMetadata(
          names = metadata.names,
          span = span,
          days = byCustom(days),
          daysCombined = byCustom(daysCombined),
          aliyot = aliyahElements.map(element => Xml.parseEmpty(element, "aliyah", parseNumbered)),
          maftir = Xml.parseEmpty(maftirElements.head, "maftir", parseSemiResolved)
        )
      }

    override def names: Map[Parsha, Names] = mapValues(get)(_.names)

    def span: Map[Parsha, Span] = Collections.inSequence(
      keys = book.parshiot,
      map = mapValues(get)(_.span),
      f = (pairs: Seq[(Parsha, SpanSemiResolved)]) =>
        SpanSemiResolved.setImpliedTo(pairs.map(_._2), book.chapters.full, book.chapters)
    )

    def days: Map[Parsha, Torah.Customs] = get.map { case (parsha, metadata) =>
      parsha -> Torah.processDays(book, metadata.days, parsha.span)
    }

    def daysCombined: Map[Parsha, Option[Torah.Customs]] = Collections.inSequence(
      keys = book.parshiot,
      map = get.map { case (parsha: Parsha, metadata: ParshaMetadata) => parsha -> metadata.daysCombined },
      f = combineDays
    )

    def aliyot: Map[Parsha, Torah] = get.map { case (parsha, metadata) =>
      val aliyot = metadata.aliyot
      val bookSpan = Torah.inBook(parsha.book,
        Span(
          parsha.span.from,
          aliyot.last.what.to.getOrElse(parsha.days.common.spans.head.span.to)
        )
      )
      parsha -> Torah.parseAliyot(bookSpan, aliyot, number = Some(3))
    }

    def maftir: Map[Parsha, Maftir] = get.map { case (parsha, metadata) =>
      val maftir = metadata.maftir
      val book = parsha.book
      val span = Span(maftir.from, maftir.to.getOrElse(parsha.span.to))

      val result = Torah.inBook(parsha.book,
        SpanSemiResolved.setImpliedTo(
          Seq(maftir),
          span,
          book.chapters
        ).head
      )

      parsha -> result
    }
  }

  private final case class DayParsed(
    span: Torah.Numbered,
    custom: Set[Custom],
    isCombined: Boolean
  )

  private def parseDay(element: Elem): DayParsed = {
    val attributes = Xml.openEmpty(element, "day")
    val result = DayParsed(
      span = parseNumbered(attributes),
      custom = attributes.get("custom").fold[Set[Custom]](Set(Custom.Common))(Custom.parse),
      isCombined = attributes.doGetBoolean("combined")
    )
    attributes.close()
    result
  }

  private def parseNumbered(attributes: Attributes): Torah.Numbered =
    WithNumber.parse(attributes, parseSemiResolved)

  private def parseSemiResolved(attributes: Attributes): SpanSemiResolved = SpanParsed.parse(attributes).semiResolve

  private def combineDays(weeks: Seq[(Parsha, Custom.Sets[Seq[Torah.Numbered]])]): Seq[Option[Torah.Customs]] = weeks match {
    case (parsha, days) :: (parshaNext, daysNext) :: tail =>
      val result: Option[Torah.Customs] = if (!parsha.combines) None else  {
        val combined: Custom.Sets[Seq[Torah.Numbered]] = daysNext ++ days.map { case (customs, value) =>
          (customs, value ++ daysNext.getOrElse(customs, Seq.empty))
        }

        val book = parsha.book
        Some(Torah.processDays(book, combined, book.chapters.merge(parsha.span, parshaNext.span)))
      }

      result +: combineDays((parshaNext, daysNext) +: tail)

    case (parsha, _ /*days*/) :: Nil =>
      require(!parsha.combines)
      Seq(None)

    case Nil => Nil
  }
}

package org.opentorah.angles

import org.opentorah.numbers.{Digit, Digits, DigitsDescriptor, PeriodicNumbers}

trait Angles extends PeriodicNumbers {

  trait Angle[N <: Angle[N]] extends Number[N] { this: N =>
    def degrees: Int = get(Digit.DEGREES)

    def degrees(value: Int): N = set(Digit.DEGREES, value)

    def roundToDegrees: N = roundTo(Digit.DEGREES)

    def minutes: Int = get(Digit.MINUTES)

    def minutes(value: Int): N = set(Digit.MINUTES, value)

    def roundToMinutes: N = roundTo(Digit.MINUTES)

    def seconds: Int = get(Digit.SECONDS)

    def seconds(value: Int): N = set(Digit.SECONDS, value)

    def roundToSeconds: N = roundTo(Digit.SECONDS)

    def thirds: Int  = get(Digit.THIRDS)

    def thirds(value: Int): N = set(Digit.THIRDS, value)

    def roundToThirds: N = roundTo(Digit.THIRDS)

    def toRadians: Double = math.toRadians(toDegrees)

    def toDegrees: Double = toDouble
  }

  trait AngleCompanion[N <: Angle[N]] extends NumberCompanion[N] {
    final def fromRadians(value: Double, length: Int): N = fromDegrees(math.toDegrees(value), length)

    final def fromDegrees(value: Double, length: Int): N = fromDouble(value, length)
  }

  final class RotationAngle(digits: Digits) extends VectorNumber(digits) with Angle[RotationAngle] {
    override def companion: RotationCompanion = Vector
  }

  final override type Vector = RotationAngle

  final type Rotation = Vector

  final class RotationCompanion extends VectorCompanion with AngleCompanion[Rotation] {
    override protected def newNumber(digits: Digits): Vector = new RotationAngle(digits)
  }

  final override lazy val Vector: RotationCompanion = new RotationCompanion

  final val Rotation = Vector

  final class PositionAngle(digits: Digits) extends PointNumber(digits) with Angle[PositionAngle] {
    override def companion: PositionCompanion = Point
  }

  final override type Point = PositionAngle

  final type Position = Point

  final class PositionCompanion extends PointCompanion with AngleCompanion[Position] {
    override protected def newNumber(digits: Digits): Point = new PositionAngle(digits)
  }

  final override lazy val Point: PositionCompanion = new PositionCompanion

  final val Position = Point

  final override def headRange: Int = 360

  final override val maxLength: Int = 10

  final override def range(position: Int): Int = 60

  object Digit extends DigitsDescriptor {
    object DEGREES extends DigitBase("°")
    object MINUTES extends DigitBase("′")
    object SECONDS extends DigitBase("″")
    object THIRDS  extends DigitBase("‴")

    override val values: Seq[Digit] = Seq(DEGREES, MINUTES, SECONDS, THIRDS)
  }
}

object Angles extends Angles // TODO if I make Angles trait itself an object, RotationTest fails!!!
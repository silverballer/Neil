package io.enoble.svg2d.parsing

import io.enoble.svg2d.parsing.Path.PathCommand

import scala.util.parsing.combinator._
import scalaz._
import Scalaz._
import scalaz.std.list._

object PathParser extends Model {

  import Path._
  import xml.Elem

  override def isDefinedAt(x: Elem): Boolean = x.label =~= "path"

  override def apply(v1: Elem): Option[Code] = {
    val pathCoords = v1.getOpt("d")
    pathCoords map { coords =>
      Path(MoveTo((1, 2)))
    }
  }
}

object Path extends JavaTokenParsers {
  type Coords = (Double, Double)
  sealed trait PathCommand
  trait Absolute
  trait Relative
  case class ClosePath() extends PathCommand
  case class MoveTo(point: Coords) extends PathCommand
  case class MoveToRel(point: Coords) extends PathCommand
  case class LineTo(point: Coords) extends PathCommand
  case class LineToRel(point: Coords) extends PathCommand
  case class VerticalLineTo(y: Double) extends PathCommand
  case class VerticalLineToRel(y: Double) extends PathCommand
  case class HorizLineTo(x: Double) extends PathCommand
  case class HorizLineToRel(x: Double) extends PathCommand
  case class Cubic(params: List[(Coords, Coords, Coords)]) extends PathCommand
  case class CubicRel(params: List[(Coords, Coords, Coords)]) extends PathCommand
  case class SmoothCubic(params: List[(Coords, Coords, Coords)]) extends PathCommand
  case class SmoothCubicRel(params: List[(Coords, Coords, Coords)]) extends PathCommand
  case class Quad(params: List[(Coords, Coords)]) extends PathCommand
  case class QuadRel(params: List[(Coords, Coords)]) extends PathCommand
  case class EllipticParam(r: Coords, rotX: Double, largeArc: Boolean, sweep: Boolean, p: Coords)
  case class Elliptic(params: List[EllipticParam])
  case class EllipticRel(params: List[EllipticParam])

  object Parsers extends JavaTokenParsers {

    import scala.language.implicitConversions
    import scalaz.std.list._

    // I'd like to know why I need to do this. I don't want to have a billion (uncomposable) abstractions
    // to switch between every time I go between the parser world and the real world
    implicit def tildeToTupleFun[A, B, C](tf: ((A, B)) => C): ((~[A, B]) => C) = (t: ~[A, B]) => tf((t._1, t._2))
    implicit def tildeToTuple2[A, B](tf: ~[A, B]): (A, B) = (tf._1, tf._2)
    implicit def tildeToTuple3[A, B, C](tf: ~[~[A, B], C]): (A, B, C) = (tf._1._1, tf._1._2, tf._2)
    implicit def tildeToTuple4[A, B, C, D](tf: ~[~[A, B], ~[C, D]]): ((A, B), (C, D)) = ((tf._1._1, tf._1._2), (tf._2._1, tf._2._2))
    implicit def tildeToTuple4K[F[_] : Functor, A, B, C, D](tf: F[~[~[A, B], ~[C, D]]]): F[((A, B), (C, D))] = tf.map(tildeToTuple4)
    def makeTwoArg[T](f: (List[(Coords, Coords)]) => T) = (a: List[~[~[Double, Double], ~[Double, Double]]]) => f(tildeToTuple4K(a))
    def makeThreeArg[T](f: (List[(Coords, Coords, Coords)]) => T) = {
      (a: List[~[~[~[Double, Double], ~[Double, Double]], ~[Double, Double]]]) => {
        f(a.map { case c1 ~ c2 ~ c => (c1, c2, c) })
      }
    }

    def parsedDouble = floatingPointNumber ^^ (_.toDouble)
    def wsp = whiteSpace
    def commaWsp = (wsp.+ ~> ",".? ~> wsp.*) | ("," ~> wsp.*)
    def wspDouble = wsp.* ~> parsedDouble

    def coordPair: Parser[~[Double, Double]] = (parsedDouble <~ commaWsp.?) ~ parsedDouble
    def wspPair: Parser[~[Double, Double]] = coordPair <~ commaWsp.?
    def twoCoordPairs: Parser[~[~[Double, Double], ~[Double, Double]]] = wspPair ~ coordPair
    def threeCoordPairs = wspPair ~ wspPair ~ coordPair
    def wspAndCoordPairs: Parser[~[~[Double, Double], ~[Double, Double]]] = commaWsp.* ~> twoCoordPairs
    def wspAndCoord: Parser[~[Double, Double]] = commaWsp.* ~> coordPair
    def lineTo: Parser[PathCommand] = (("L" ~> wspAndCoord) ^^ LineToRel) | (("l" ~> wspAndCoord) ^^ LineTo)
    def horizLineTo: Parser[PathCommand] = (("h" ~> wspDouble) ^^ HorizLineToRel) | (("H" ~> wspDouble) ^^ HorizLineTo)
    def vertLineTo: Parser[PathCommand] = (("v" ~> wspDouble) ^^ VerticalLineToRel) | (("V" ~> wspDouble) ^^ VerticalLineTo)
    def moveTo: Parser[PathCommand] = ("M" ~> wspAndCoord) ^^ MoveTo
    // TODO: Find out what this is for
    //def moveToRel: Parser[PathCommand] = ("m" ~> spaceAndCoords) ^^ MoveToRel
    def quadArgs = repsep(twoCoordPairs, commaWsp.?)
    def quad: Parser[PathCommand] = ("q" ~> quadArgs ^^ makeTwoArg(Quad)) | ("Q" ~> quadArgs ^^ makeTwoArg(QuadRel))
//    def ellipticArgs: Parser[EllipticParam] =
//    def ellipticalArc: Parser[PathCommand] = ("a" ~> ellipticArgs ^^ Elliptic) | ("A" ~> ellipticArgs ^^ EllipticRel)
    def cubicArgs = repsep(threeCoordPairs, commaWsp.?)
    def cubic: Parser[PathCommand] = ("c" ~> cubicArgs ^^ makeThreeArg(Cubic)) | (("C" ~> cubicArgs) ^^ makeThreeArg(CubicRel))
    def smoothCubic: Parser[PathCommand] = ("s" ~> cubicArgs ^^ makeThreeArg(SmoothCubic)) | ("S" ~> cubicArgs ^^ makeThreeArg(SmoothCubicRel))
    def closePath: Parser[PathCommand] = "z" ^^ (_ => ClosePath())
    def command: Parser[PathCommand] = closePath | lineTo | horizLineTo | vertLineTo | cubic | smoothCubic | quad //| ellipticalArc
    def path: Parser[~[PathCommand, Seq[PathCommand]]] = (moveTo <~ commaWsp) ~ command.*
  }

}


case class Path(commands: PathCommand*) extends Code {

  import Path._

  override def toAndroidCode: AndroidCode =
    java"""
      {
      Path path = new Path()
      ${
      commands.foldLeft("") { (str, cmd) =>
        str + "\n" + (cmd match {
          case LineTo((x, y)) => s"path.lineTo($x, $y)"
          case LineToRel((x, y)) => s"path.rLineTo($x, $y)"
          case MoveTo((x, y)) => s"path.moveTo($x, $y)"
          case MoveToRel((x, y)) => s"path.rMoveTo($x, $y)"
          case ClosePath() => s"path.close()"
        })
      }
    }
      }
    """.stripMargin

  override def toIOSCode: IOSCode = {
    ""
  }
}

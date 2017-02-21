package io
package enoble
package svg2d

import io.enoble.svg2d.ast.FinalSVG

import scala.util.Try

package object xmlparse {

  trait Model {
    val label: String
    def apply[A](elem: xml.Elem, svg: FinalSVG[A]): Option[A]
  }

  type ParseError = Exception

  implicit class HasAttributes(val x: xml.Elem) extends AnyVal {
    def getOrDefault(n: String, default: String): String = x.attribute(n).map(_.head.text).getOrElse(default)

    def getOpt(n: String): Option[String] = x.attribute(n).map(_.head.text)
  }

  implicit class CaseInsensitiveEquals(val s: String) extends AnyVal {
    def ~=(other: String): Boolean = other.equalsIgnoreCase(s)
  }

  implicit class ParseDouble(val s: String) extends AnyVal {
    def asDouble: Option[Double] = Try(s.toDouble).toOption
  }

  implicit class Ignorable[T](val t: T) extends AnyVal {
    def ignore(): Unit = ()
  }

  implicit class CombinePartial[T, R](val partial: PartialFunction[T, R]) extends AnyVal {
    def makeTotal(total: (T) => R): T => R = partial orElse { case x => total(x) }
  }

  implicit class JavaHelper(val sc: StringContext) extends AnyVal {
    def java(args: Any*): String = ("\n" + sc.parts.mkString).split("\n").mkString(";\n").trim()
  }

}

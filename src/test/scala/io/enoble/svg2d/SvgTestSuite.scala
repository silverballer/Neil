package io
package enoble
package svg2d

import io.enoble.svg2d.ast.FastMonoid
import io.enoble.svg2d.xmlparse.Parse
import io.enoble.svg2d.render.{InitialCode, InitialRenderer}
import org.scalatest.FunSuite

abstract class SvgTestSuite extends FunSuite {
//  def verifySingle(svg: String, cmd: InitialCode): Unit = {
//    val svgXml = xml.XML.loadString(svg)
//    assert(Parse.parseAll(InitialRenderer(FastMonoid.Vec()))(svgXml) === Some(cmd))
//  }
}
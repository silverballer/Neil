package io.enoble.svg2d

import java.io.File
import java.nio.file.{Files, Paths}
import java.text.DecimalFormat

import io.enoble.svg2d.parsing.{CodeGenerator, Parse}
import scopt.Read

import scalaz._
import Scalaz._
import scala.io.Source

import scala.collection.JavaConverters._

object Main {
  sealed trait OutputType
  case object Swift extends OutputType
  case object ObjectiveC extends OutputType
  case object Android extends OutputType
  case object Raw extends OutputType

  val outputTypeMapping: Map[String, Main.OutputType] = List(
    List("s", "swift") -> Swift,
    List("c", "objc", "objectivec") -> ObjectiveC,
    List("a", "android", "j", "java") -> Android,
    List("r", "raw") -> Raw
  ).flatMap(p => p._1.map(_ -> p._2))(collection.breakOut)

  def parseOutputType(str: String): OutputType =
    outputTypeMapping.getOrElse(str, throw new IllegalArgumentException(s"$str is not a valid output type: try 's', 'c', 'a', or 'r'"))

  case class MainConfig(outputType: OutputType = null, inputFolder: File = null, outputFolder: Option[File] = None)

  implicit val outputTypeInstances: Read[OutputType] = Read.reads(parseOutputType)

  val parser = new scopt.OptionParser[MainConfig]("neil") {
      head("neil", "0.0.1")
      opt[OutputType]('t', "otype") required() valueName "<outputType>" action { (x, c) =>
        c.copy(outputType = x)
      } text "output type; valid output types are 's' (Swift), 'c' (Objective C), 'a' (Android), and 'r' (Raw)"
      opt[File]('i', "input") required() valueName "<file>" action { (x, c) =>
        c.copy(inputFolder = x) } text "input folder of svg's"
      opt[File]('o', "output") optional() valueName "<file>" action { (x, c) =>
        c.copy(outputFolder = Some(x)) } text "output code generation folder (or none, for stdout)"
      help("help") text "prints this usage text"
  }

  def main(args: Array[String]): Unit = {
    val configParsed = parser.parse(args, MainConfig())
    configParsed.foreach { config =>
      val filePath = config.inputFolder
      val isDir = filePath.isDirectory
      if (isDir) {
        val svgFiles = filePath.listFiles()
        val xmlFiles = svgFiles.map(f => xml.XML.loadFile(f))
        val parsed = xmlFiles.map(Parse.parseAll)
        val (successes, failures) = parsed.partition(_.isDefined)
        val successCount = successes.length
        val failureCount = failures.length
        val successRate = (successCount * 100) / (successCount + failureCount).toDouble
        println(s"Successes: $successCount")
        println(s"Failures: $failureCount")
        println(f"Success rate: $successRate%2.2f%%")
        val failedFiles = parsed.zipWithIndex.map(x => (x._1, svgFiles(x._2))).filter(_._1.isEmpty).map(_._2.getName)
        println(s"Failed files: \n${failedFiles.mkString("\n")}")
        println(parsed.toVector)
      } else {
        val xml = scala.xml.XML.loadFile(filePath)
        val parsed = Parse.parseAll(xml)
        parsed.fold {
          println("Parsing failed!")
        } { codes =>
          val output = config.outputType match {
            case Swift => CodeGenerator.generateSwiftCode(codes)
            case ObjectiveC => CodeGenerator.generateObjCCode(codes)
            case Android => CodeGenerator.generateAndroidCode(codes)
            case Raw => codes
          }
          println(output)
        }
      }
    }
  }
}


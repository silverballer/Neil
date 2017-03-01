name in ThisBuild := "neil"

scalaVersion in ThisBuild := "2.12.1"

scalaOrganization in ThisBuild := "org.typelevel"

version in ThisBuild := "0.0.2"

scalacOptions ++= Seq(
  "-Xlint",
  "-Ypartial-unification",
  "-Yliteral-types",
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:higherKinds",
  "-Xfatal-warnings"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

libraryDependencies += "org.typelevel" %% "cats" % "0.9.0"

libraryDependencies += "io.monix" %% "monix" % "2.2.2"

libraryDependencies += "io.monix" %% "monix-cats" % "2.2.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

libraryDependencies += "com.lihaoyi" %% "fastparse" % "0.4.2"

libraryDependencies += "com.github.alexarchambault" %% "case-app" % "1.2.0-M1"

test in assembly := {}

mainClass in assembly := Some("io.enoble.svg2d.Main")

resolvers += Resolver.sonatypeRepo("public")

javacOptions += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

fork in test := true
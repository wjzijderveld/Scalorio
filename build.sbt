name := "factorio-helper"

version := "1.0"

scalaVersion := "2.12.1"

javaHome := Some(file("C:\\Program Files\\Java\\jdk1.8.0_121"))

mainClass in Compile := Some("net.willemjan.factorio.calculator.Main")

libraryDependencies ++= Seq(
  "org.luaj" % "luaj-jse" % "2.0.3",
  "org.json4s" %% "json4s-native" % "3.5.1",
  "org.scala-lang.modules" %% "scala-swing" % "2.0.0"
)
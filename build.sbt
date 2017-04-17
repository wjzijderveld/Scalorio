name := "factorio-helper"

version := "1.0"

scalaVersion := "2.12.1"

mainClass in Compile := Some("net.willemjan.factorio.calculator.Main")

libraryDependencies ++= Seq(
  "org.luaj" % "luaj-jse" % "2.0.3"
)
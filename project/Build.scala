import sbt._
import Keys._
import sbtassembly.AssemblyPlugin.autoImport._

object GridBuild extends Build {

  import Dependencies._

  lazy val gridApp = Project(
    id = "vgs",
    base = file("./"),
    settings = commonSettings ++ assemblySettings ++ Seq(
      mainClass in assembly := Some("grid.main"),
      libraryDependencies ++= loggingDeps ++ testDeps
    )
  )

  lazy val commonSettings = Seq(
    organization := "santiagoAndFerdy",
    version := "1.0.0",
    scalaVersion := "2.11.7"
  )

  lazy val assemblySettings = Seq(
    target in assembly := file("out/")
  )
}
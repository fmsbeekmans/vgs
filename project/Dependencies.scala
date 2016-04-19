import sbt._
import Keys._

object Dependencies {
  lazy val loggingDeps = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7")

  lazy val testDeps = Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % "test")
}
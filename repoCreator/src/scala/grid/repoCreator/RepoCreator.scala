package grid.repoCreator

import java.io.{File, PrintWriter}

import jline.console.ConsoleReader

import scala.collection.mutable._
import scala.util.Try
import scala.util.control.Breaks._

object RepoCreator extends App {
  val reader = new ConsoleReader()
  val ips = ListBuffer[String]()
  var users = 0
  var rms = 0
  var gss = 0

  breakable {
    while(true) {
      val line = reader.readLine()
      val parts = line.split(' ').toList
      val arg = parts.drop(1).toList.mkString(" ")

      parts.headOption.foreach {
        case "q" => break

        case "ip" => {
          if (arg.length > 0) {
            ips += arg
          } else {
            println("No adrress given")
          }
        }
        case "ips" => {
          ips.foreach(println)
        }
        case "u" => {
          val n = Try { arg.toInt }.toOption

          if(n.isDefined) {
            users = n.get
          } else {
            println(s"${arg} is not a number")
          }
        }
        case "gs" => {
          val n = Try { arg.toInt }.toOption

          if(n.isDefined) {
            users = n.get
          } else {
            println(s"${arg} is not a number")
          }
        }
        case "rm" => {
          val n = Try { arg.toInt }.toOption

          if(n.isDefined) {
            users = n.get
          } else {
            println(s"${arg} is not a number")
          }
        }
        case "write" => {
          write()
        }
        case "help" => {
          println(
            s"""
               |:q      exit
               |ip <ip> add machine <ip>
               |ips     list ips
               |u       set the number of users per machine
               |rm      set the number of resource managers per machine
               |gs      set the number of grid schedulers per machine
               |help    show this screen
               |write   write the manifests and repositories
             """.stripMargin)
        }
        case cmd => {
          println(s"unknown command ${cmd}")
        }
      }
    }
  }

  def write(): Unit = {
    // write users
    val userWriter = new PrintWriter(new File("users"))
    val users = (for {
      ip <- ips
      u <- 0 until users
    } yield (users, ip)).zipWithIndex



//    val userWriter = new PrintWriter(new File("users"))
//    val userWriter = new PrintWriter(new File("users"))
  }
}
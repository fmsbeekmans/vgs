package grid.discovery

import java.io.File

import collection.mutable._
import java.nio.file.{Files, Path, Paths}
import java.rmi.Naming
import java.util.Scanner

import grid.cluster.Addressable
import grid.discovery.Selector.Selector
import grid.gridScheduler.IGridScheduler
import grid.resourceManager.IResourceManager
import grid.user.IUser

import scala.util.Try
import scala.util.control.Breaks._

class Repository[T <: Addressable](registry: collection.immutable.Map[Int, String]) {

  var silent = false

  private val isOnline: Map[Int, Boolean] = Map()
  val load: Map[Int, Long] = Map()

  registry.keys.foreach(id => {
    isOnline.put(id, true)
    load.put(id, 0L)
  })

  def ids(): collection.immutable.Seq[Int] = registry.keys.toList

  def url(id: Int): String = {
    registry(id)
  }

  val urls: collection.immutable.Seq[String] = registry.values.toList

  val onlineCallbacks: ListBuffer[Int => Unit] = ListBuffer()
  val offlineCallbacks: ListBuffer[Int => Unit] = ListBuffer()

  def setStatus(id: Int, online: Boolean) = synchronized {
    val oldState = isOnline(id)
    val newState = online

    isOnline.put(id, online)

    if (!silent && oldState != newState) {
      silent = true
      if (!newState) {
        // went offline
        offlineCallbacks.foreach(f => f(id))
      } else {
        // went online
        onlineCallbacks.foreach(f => f(id))
      }
      silent = false
    }
  }

  def onlineIds(): collection.immutable.Seq[Int] = {
    isOnline
      .collect { case (id, true) => id }
      .toList
  }

  def getEntity(id: Int): Option[T] = {
    val result = Try { Naming.lookup(url(id)).asInstanceOf[T] }.toOption

    result.flatMap(e => {
      Try {
        e.ping()
      }.toOption
    }) match {
      case Some(ping) => {
        setStatus(id, true)
        load.put(id, ping)
      }
      case None => {
        setStatus(id, false)
        load.put(id, Integer.MAX_VALUE)
      }
    }

    result
  }

  def invokeOnEntity[R](f:(T, Int) => R, selector: Selector, excludeIds: Int*): Option[(R, Int)] = {
    var result: Option[(R, Int)] = None
    val afterExclude = load.filter(idAndWeight => onlineIds.contains(idAndWeight._1) && !excludeIds.contains(idAndWeight._1)).toMap

    breakable {
      while (ids.nonEmpty) {
        result = for {
          id <- {
            val id = selector.selectIndex(afterExclude)

            if(excludeIds.contains(id)) println("CLASH")

            id
          }
          entity <- getEntity(id)
          response = f(entity, id)
        } yield (response, id)

        if (result.isDefined) break

        var ids = (onlineIds.diff(excludeIds)).nonEmpty
      }
    }

    result
  }

  def onOnline(f: Int => Unit): Unit = synchronized {
    onlineCallbacks += f
  }

  def onOffline(f: Int => Unit): Unit = synchronized {
    offlineCallbacks += f
  }

  def checkStatus(of: Int): Boolean = {
    Try {
      Naming.lookup(url(of)).asInstanceOf[T]
    }.flatMap { entity =>
      Try { entity.ping() }
    }.toOption.isDefined
  }
}

object Repository {
  def fromFile[T <: Addressable](fileName: String): Repository[T] = {
    val scanner = new Scanner(new File(fileName))

    val registry: Map[Int, String] = Map()

    while(scanner.hasNextLine()) {
      val line = scanner.nextLine()
      val parts = line.split(" ")

      registry.put(parts.head.toInt, parts(1))
    }

    new Repository[T](registry.toMap);
  }


  def gsRepo(fileName: String) = {
    Repository.fromFile[IGridScheduler](fileName)
  }

  def rmRepo(fileName: String) = {
    Repository.fromFile[IResourceManager](fileName)
  }

  def userRepo(fileName: String) = {
    Repository.fromFile[IUser](fileName)
  }
}
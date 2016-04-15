package grid.discovery

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

  private var silent = false

  private val isOnline: Map[Int, Boolean] = Map()
  val load: Map[Int, Long] = Map()

  registry.keys.foreach(id => {
    isOnline.put(id, true)
    load.put(id, 0L)
  })

  def ids(): collection.immutable.Seq[Int] = synchronized { registry.keys.toList }

  def url(id: Int): String = synchronized { urls().toList(id) }
  def urls(): collection.immutable.Seq[String] = synchronized { registry.values.toList }

  val onlineCallbacks: ListBuffer[Int => Unit] = synchronized { ListBuffer() }
  val offlineCallbacks: ListBuffer[Int => Unit] = synchronized { ListBuffer() }

  def setStatus(id: Int, online: Boolean) = synchronized {
    val oldState = isOnline(id)
    val newState = online

    isOnline.put(id, online)

    synchronized {
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
  }

  def onlineIds(): collection.immutable.Seq[Int] = synchronized {
    isOnline
      .collect { case (id, true) => id }
      .toList
  }

  def getEntity(id: Int): Option[T] = synchronized {
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

  def invokeOnEntity[R](f:(T, Int) => R, selector: Selector, excludeIds: Int*): Option[(R, Int)] = synchronized {
    var result: Option[(R, Int)] = None
    val afterExclude = load.filter(idAndWeight => onlineIds.contains(idAndWeight._1) && !excludeIds.contains(idAndWeight._1)).toMap

    breakable {
      while ((onlineIds.diff(excludeIds)).nonEmpty) {
        result = for {
          id <- selector.selectIndex(afterExclude)
          entity <- getEntity(id)
          response = f(entity, id)
        } yield (response, id)

        if (result.isDefined) break
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
  def fromFile[T <: Addressable](path: Path): Repository[T] = {


    val inStream = Files.newInputStream(path)
    val scanner = new Scanner(inStream)

    val registry: Map[Int, String] = Map()

    while(scanner.hasNextLine()) {
      val line = scanner.nextLine()
      val parts = line.split(" ")

      registry.put(parts.head.toInt, parts(1))
    }

    new Repository[T](registry.toMap);
  }


  val gsRepo = {
    Repository.fromFile[IGridScheduler](resourcePath("gss"))
  }

  val rmRepo = {
    println("repo")
    Repository.fromFile[IResourceManager](resourcePath("rms"))
  }

  val userRepo = {
    Repository.fromFile[IUser](resourcePath("users"))
  }


  def resourcePath(fileName: String): Path = {
    val url = getClass.getClassLoader.getResource(fileName)
    Paths.get(url.toURI)
  }
}
package grid.discovery

import collection._
import java.nio.file.{Files, Path}
import java.rmi.Naming
import java.util.Scanner

import grid.discovery.Selector.Selector
import grid.rmi.{Addressable, RmiServer}

import scala.concurrent.Future
import scala.util.Try
import scala.util.control.Breaks._

class Repository[T <: Addressable](registry: Map[Int, String]) {

  val isOnline: mutable.Map[Int, Boolean] = mutable.Map()
  val load: mutable.Map[Int, Long] = mutable.Map()

  registry.keys.foreach(id => {
    isOnline.put(id, true)
    load.put(id, 0L)
  })

  def ids(): Seq[Int] = registry.keys.toList

  def url(id: Int): String = urls().toList(id)
  def urls(): Seq[String] = registry.values.toList

  def onlineCallbacks: mutable.Seq[Int => Void] = mutable.ListBuffer()
  def offlineCallbacks: mutable.Seq[Int => Void] = mutable.ListBuffer()

  def setStatus(id: Int, online: Boolean) = isOnline.put(id, online)

  def onlineIds(): Seq[Int] = isOnline
    .collect { case (id, true) => id }
    .toList

  def getEntity(id: Int): Option[T] = {
    val result = Try { Naming.lookup(url(id)).asInstanceOf[T] }.toOption
    val oldState = isOnline(id)
    val newState = result.isDefined

    if(oldState != newState) {
      if(!newState) {
        // went offline
        offlineCallbacks.foreach(f => f(id))
      } else {
        // went online
        onlineCallbacks.foreach(f => f(id))
      }
    }

    result
  }

  def invokeOnEntity[R](f:(T, Int) => R, selector: Selector, excludeIds: Int*): Option[(R, Int)] = {
    var result: Option[(R, Int)] = None
    val afterExclude = load.filter(idAndWeight => !excludeIds.contains(idAndWeight._1)).toMap

    breakable {
      while (onlineIds.nonEmpty) {
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

  def onOnline(f: Int => Unit): Unit = {
    onlineCallbacks :+ f
  }

  def onOffline(f: Int => Unit): Unit = {
    offlineCallbacks :+ f
  }
}

object Repository {
  def fromFile[T <: Addressable](path: Path): Repository[T] = {


    val inStream = Files.newInputStream(path)
    val scanner = new Scanner(inStream)

    val registry: mutable.Map[Int, String] = mutable.Map()

    while(scanner.hasNextLine()) {
      val line = scanner.nextLine()
      val parts = line.split(" ")

      registry.put(parts.head.toInt, parts(1))
    }

    new Repository[T](registry.toMap);
  }
}
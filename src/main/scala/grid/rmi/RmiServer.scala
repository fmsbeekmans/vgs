package grid.rmi

import java.rmi.Naming
import java.rmi.registry._

import grid.cluster.Addressable

import scala.util.Try

/**
  * Created by Fydio on 4/13/16.
  */
object RmiServer {

  val rmiPort = 1099
  var registery: Registry = Try {
    LocateRegistry.createRegistry(rmiPort)
  }.toOption.getOrElse(LocateRegistry.getRegistry(rmiPort))

  def register(entity: Addressable): Unit = {
    val bindResult = Naming.rebind(entity.url, entity)
    println(s"Bound: $bindResult")
  }

}

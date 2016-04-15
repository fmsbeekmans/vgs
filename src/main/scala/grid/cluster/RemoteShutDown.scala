package grid.cluster

import java.rmi.RemoteException

trait RemoteShutDown extends Addressable {
  @throws(classOf[RemoteException])
  def online: Boolean

  @throws(classOf[RemoteException])
  def start(): Unit

  @throws(classOf[RemoteException])
  def shutDown(): Unit

  @throws(classOf[RemoteException])
  def ifOnline[T](then: => T): T = {
    if(online) {
      then
    } else {
      throw new RemoteException("$id is offline")
    }
  }
}

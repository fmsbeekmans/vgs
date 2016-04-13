package grid.discovery

import java.rmi.RemoteException

import grid.rmi.Addressable

trait RemoteShutDown extends Addressable {
  @throws(classOf[RemoteException])
  def start(): Unit

  @throws(classOf[RemoteException])
  def shutDown(): Unit

}

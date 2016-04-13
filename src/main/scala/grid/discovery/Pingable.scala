package grid.discovery

import java.rmi.RemoteException

import grid.rmi.Addressable

trait Pingable extends Addressable {
  @throws(classOf[RemoteException])
  def ping(): Long
}

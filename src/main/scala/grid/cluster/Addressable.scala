package grid.cluster

import java.rmi.{Remote, RemoteException}

trait Addressable extends Remote {

  @throws(classOf[RemoteException])
  def id: Int

  @throws(classOf[RemoteException])
  def ping(): Long

  @throws(classOf[RemoteException])
  def url: String
}

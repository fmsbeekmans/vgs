package grid.rmi

import java.rmi.{Remote, RemoteException}

trait Addressable extends Remote {

  @throws(classOf[RemoteException])
  def id: Int

  @throws(classOf[RemoteException])
  def url: String
}

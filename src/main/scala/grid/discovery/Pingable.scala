package grid.discovery

import java.rmi.{Remote, RemoteException}

trait Pingable extends Remote {
  @throws(classOf[RemoteException])
  def ping(): Long
}

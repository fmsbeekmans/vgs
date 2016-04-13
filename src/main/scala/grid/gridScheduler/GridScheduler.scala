package grid.gridScheduler

import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

import grid.discovery.Repository
import grid.messages.{BackUpRequest, MonitorRequest, PromoteRequest}
import grid.resourceManager.IResourceManager
import grid.rmi.RmiServer

class GridScheduler(val id: Int,
                    val rmRepo: Repository[IResourceManager],
                    val gsRepo: Repository[IGridScheduler]) extends UnicastRemoteObject with IGridScheduler {

  RmiServer.register(this)

  @throws(classOf[RemoteException])
  override def monitor(req: MonitorRequest): Unit = {
    println("monitor")
  }

  @throws(classOf[RemoteException])
  override def backUp(req: BackUpRequest): Unit = {
    println("backup")
  }

  @throws(classOf[RemoteException])
  override def promote(req: PromoteRequest): Unit = {

  }

  @throws(classOf[RemoteException])
  override def url: String = gsRepo.url(id)
}

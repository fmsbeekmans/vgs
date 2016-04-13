package grid.user

import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

import grid.discovery.Repository
import grid.messages.WorkRequest
import grid.model.Job
import grid.resourceManager.IResourceManager
import grid.rmi.RmiServer

import scala.concurrent.duration.Duration

class User(val id: Int,
           val userRepo: Repository[IUser],
           val rmRepo: Repository[IResourceManager]) extends UnicastRemoteObject with IUser {

  RmiServer.register(this)

  @throws(classOf[RemoteException])
  override def createJobs(rmId: Int, n: Int, ms: Int): Unit = {
    for { i <- 0 until n }{
      rmRepo.getEntity(rmId).foreach(rm => {
        val req = WorkRequest(Job(i, rmId, ms), id)
        rm.offerWork(req)
      })
    }
  }

  @throws(classOf[RemoteException])
  override def acceptResult(job: Job): Unit = {
    println(job)
  }

  override def url: String = {
    userRepo.urls().toList.apply(id)
  }
}

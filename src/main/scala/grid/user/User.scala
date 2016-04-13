package grid.user

import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

import grid.discovery.Repository
import grid.messages.WorkRequest
import grid.model.Job
import grid.resourceManager.IResourceManager
import grid.rmi.RmiServer

import scala.collection.mutable._

class User(val id: Int,
           val userRepo: Repository[IUser],
           val rmRepo: Repository[IResourceManager]) extends UnicastRemoteObject with IUser {

  RmiServer.register(this)

  var jobs = 0

  @throws(classOf[RemoteException])
  override def createJobs(rmId: Int, n: Int, ms: Int): Unit = {
    jobs += n

    for { i <- 0 until n }{
      rmRepo.getEntity(rmId).foreach(rm => {
        val job = Job(i, rmId, ms)

        val req = WorkRequest(job, id)
        rm.offerWork(req)
      })
    }
  }

  @throws(classOf[RemoteException])
  override def acceptResult(job: Job): Unit = {
    println(s"[U\t$id] Result for ${job.id}")

    jobs -= 1

    if(jobs == 0) println(s"[U\t$id] Jobs completed")
  }

  override def url: String = {
    userRepo.urls().toList.apply(id)
  }
}

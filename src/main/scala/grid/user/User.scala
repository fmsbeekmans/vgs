package grid.user

import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

import com.typesafe.scalalogging.LazyLogging
import grid.discovery.Repository
import grid.messages.WorkRequest
import grid.model.Job
import grid.resourceManager.IResourceManager
import grid.rmi.RmiServer

import scala.collection.mutable._
import scala.util.Random

class User(val id: Int,
           val userRepo: Repository[IUser],
           val rmRepo: Repository[IResourceManager]) extends UnicastRemoteObject with IUser with LazyLogging {

  RmiServer.register(this)

  var jobs = 0

  @throws(classOf[RemoteException])
  override def createJobs(rmId: Int, n: Int, ms: Int): Unit = {
    jobs += n

    for { i <- 0 until n }{
      rmRepo.getEntity(rmId).foreach(rm => {
        val job = Job(i, rmId, Random.nextInt(ms * 2))

        val req = WorkRequest(job, id)
        rm.offerWork(req)
      })
    }
  }

  @throws(classOf[RemoteException])
  override def acceptResult(job: Job): Unit = {
    synchronized(jobs -= 1)

    logger.info(s"[U\t$id] Result for ${job.id}")
//    println(s"[U\t$id] Result for ${job.id}")


    if(jobs == 0) {
      logger.info(s"[U\t$id] Jobs completed")
      logger.info(s"[U\t$id] Jobs completed")
      System.exit(0)
    }
  }

  override def url: String = {
    userRepo.urls().toList.apply(id)
  }

  def ping = 0
}

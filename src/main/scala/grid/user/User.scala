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
import scala.concurrent.Promise
import scala.util.Random

class User(val id: Int,
           val userRepo: Repository[IUser],
           val rmRepo: Repository[IResourceManager]) extends UnicastRemoteObject with IUser with LazyLogging {

  RmiServer.register(this)

  var jobs = 0
  val pendingJobs: Set[Job] = Set()
  val offeredJobs: Map[Job, Promise[Unit]] = Map()

  @throws(classOf[RemoteException])
  override def createJobs(rmId: Int, n: Int, ms: Int): Unit = {
    jobs += n

    for { i <- 0 until n }{
      rmRepo.getEntity(rmId).foreach(rm => {
        synchronized {
          val job = Job(i, rmId, Random.nextInt(ms * 2))
//          val promise = Promise[Unit]
//          promise.future

          pendingJobs += job
//          offeredJobs.put(job, )

          val req = WorkRequest(job, id)
          rm.offerWork(req)
        }
      })
    }
  }

  @throws(classOf[RemoteException])
  override def acceptJob(job: Job): Unit = {

  }

  @throws(classOf[RemoteException])
  override def acceptResult(job: Job): Unit = {
    pendingJobs.synchronized {
      if(pendingJobs.contains(job)) {
        synchronized(jobs -= 1)
        pendingJobs -= job

        logger.info(s"[U\t$id] Result for ${job.id} $jobs left")

        if (jobs == 0) {
          logger.info(s"[U\t$id] Jobs completed")
          logger.info(s"[U\t$id] Jobs completed")
          System.exit(0)
        }
      }
    }
  }

  override def url: String = {
    userRepo.urls().toList.apply(id)
  }

  def ping = 0
}

package grid.user

import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

import com.typesafe.scalalogging.LazyLogging
import grid.discovery.Repository
import grid.messages.WorkRequest
import grid.model.{IDGen, Job}
import grid.resourceManager.IResourceManager
import grid.rmi.RmiServer
import org.slf4j.LoggerFactory

import scala.collection.mutable._
import scala.concurrent.Promise
import scala.util.Random

class User(val id: Int,
           val userRepo: Repository[IUser],
           val rmRepo: Repository[IResourceManager]) extends UnicastRemoteObject with IUser with LazyLogging {

  RmiServer.register(this)

  val jobLog = LoggerFactory.getLogger("jobs")

  val pendingJobs: Set[Job] = Set()
  val offeredJobs: Map[Job, Promise[Unit]] = Map()

  @throws(classOf[RemoteException])
  override def createJobs(rmId: Int, n: Int, ms: Int): Unit = {
    for { i <- 0 until n }{
      rmRepo.getEntity(rmId).foreach(rm => {
        val job = Job(IDGen.genId(), rmId, Random.nextInt(ms * 2))

        val req = WorkRequest(job, id)
        rm.offerWork(req)
      })
    }
  }

  @throws(classOf[RemoteException])
  override def acceptJob(job: Job): Unit = {
    pendingJobs.synchronized(pendingJobs += job)

  }

  @throws(classOf[RemoteException])
  override def acceptResult(job: Job): Unit = {
    pendingJobs.synchronized {
      if(pendingJobs.contains(job)) {
        pendingJobs -= job

        val now = System.currentTimeMillis()
        jobLog.info(s"${job.id}, ${job.created}, $now, ${job.firstRmId}, ${job.otherRms.mkString(", ")}")

        logger.info(s"[U\t$id] Result for ${job.id} ${pendingJobs.size} left")

        if (pendingJobs.isEmpty) {
          logger.info(s"[U\t$id] Jobs completed")
        }
      }
    }
  }

  override def url: String = {
    userRepo.urls().toList.apply(id)
  }

  def ping = 0
}

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
import scala.util.{Random, Try}

class User(val id: Int,
           val userRepo: Repository[IUser],
           val rmRepo: Repository[IResourceManager]) extends UnicastRemoteObject with IUser with LazyLogging {

  RmiServer.register(this)

  val pendingJobs = ListBuffer[Job]()

  val jobLog = LoggerFactory.getLogger("jobs.5.20.5.20.80.10.10.40.10")

  @throws(classOf[RemoteException])
  override def createJobs(rmId: Int, n: Int, ms: Int): Unit = {
    for {
      i <- 0 until n
    }{

      rmRepo.getEntity(rmId).foreach(rm => {
        val job = Job(IDGen.genId(), rmId, ms)
        val req = WorkRequest(job, id)

        synchronized(pendingJobs += job)

        Try { rm.offerWork(req) }
      })
    }
  }

  @throws(classOf[RemoteException])
  override def acceptJob(job: Job): Unit = {
    logger.info(s"[U\t${id}] Accepted job ${job.id}")
  }

  @throws(classOf[RemoteException])
  override def acceptResult(job: Job): Unit = synchronized {
    if (pendingJobs.contains(job)) {
      pendingJobs -= job
      logger.info(s"[U\t${id}] Result for job ${job.id}")

      val now = System.currentTimeMillis()
      jobLog.info(s"${id}, ${now - job.created}, ${job.firstRmId}")
    }
  }

  override def url: String = {
    userRepo.url(id)
  }

  def ping = 0
}

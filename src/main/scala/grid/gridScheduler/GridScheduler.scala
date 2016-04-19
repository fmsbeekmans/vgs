package grid.gridScheduler

import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

import com.typesafe.scalalogging.LazyLogging
import grid.cluster.{Pinger, RemoteShutDown}
import grid.discovery.Repository
import grid.discovery.Selector.InvertedRandomWeighedSelector
import grid.messages._
import grid.resourceManager.IResourceManager
import grid.rmi.RmiServer

import collection.mutable._
import scala.util.Try
import scala.util.control.NonFatal

class GridScheduler(val id: Int,
                    val rmRepo: Repository[IResourceManager],
                    val gsRepo: Repository[IGridScheduler]) extends UnicastRemoteObject with IGridScheduler with RemoteShutDown with LazyLogging {

  RmiServer.register(this)

  var online = false

  var gsPinger = new Pinger[IGridScheduler](gsRepo)
  var rmPinger = new Pinger[IResourceManager](rmRepo)

  var monitoringForRm: Map[WorkRequest, Int] = null
  var monitoringPerRm: Map[Int, Set[WorkRequest]] = null
  var backUpForRm: Map[WorkRequest, Int] = null
  var backUpPerRm: Map[Int, Set[WorkRequest]] = null
  var backUpForGs: Map[WorkRequest, Int] = null
  var backUpPerGs: Map[Int, Set[WorkRequest]] = null

  var load = 0

  start()

  override def start(): Unit = synchronized {
    monitoringForRm = Map()
    monitoringPerRm = Map()
    backUpForRm = Map()
    backUpPerRm = Map()
    backUpForGs = Map()
    backUpPerGs = Map()

    gsRepo.ids().foreach(gsId => {
      monitoringPerRm.put(gsId, Set())
      backUpPerRm.put(gsId, Set())
      backUpPerGs.put(gsId, Set())
    })

    load = 0

    gsPinger.start()
    rmPinger.start()

    online = true

    logger.info(s"[GS\t${id}] Online")
  }

  override def shutDown(): Unit = synchronized {
    monitoringForRm = Map()
    monitoringPerRm = Map()
    backUpForRm = Map()
    backUpPerRm = Map()
    backUpForGs = Map()
    backUpPerGs = Map()

    gsRepo.ids().foreach(gsId => {
      monitoringPerRm.put(gsId, Set())
      backUpPerRm.put(gsId, Set())
      backUpPerGs.put(gsId, Set())
    })

    load = 0

    gsPinger.stop()
    rmPinger.stop()

    online = false

    logger.info(s"[GS\t${id}] Offline")
  }

  override def monitor(req: MonitorRequest): Unit = ifOnline {
    logger.info(s"[GS\t${id}] Monitoring job ${req.work.job.id}")
    registerMonitor(req.work, req.rmId)
  }

  def registerMonitor(work: WorkRequest, rmId: Int): Unit = synchronized {
    monitoringForRm.put(work, rmId)
    monitoringPerRm(rmId) += work
  }

  def unregisterMonitor(work: WorkRequest): Unit = synchronized {
    Try { monitoringPerRm(monitoringForRm(work)) -= work }
    Try { monitoringForRm -= work }
  }


  override def releaseMonitor(work: WorkRequest): Unit = ifOnline {
    logger.info(s"[GS\t${id}] Releasing monitoring for job ${work.job.id}")
    unregisterMonitor(work)
  }

  override def backUp(req: BackUpRequest): Unit = ifOnline {
    logger.info(s"[GS\t${id}] Backing up job ${req.work.job.id}")
    registerBackUp(req.work, req.rmId, req.monitorId)
  }

  def registerBackUp(work: WorkRequest, rmId: Int, monitorId: Int): Unit = synchronized {
    backUpForRm.put(work, rmId)
    backUpPerRm(rmId) += work

    backUpForGs.put(work, monitorId)
    backUpPerGs(monitorId) += work
  }

  def unregisterBackUp(work: WorkRequest): Unit = synchronized {
    Try { backUpPerRm(backUpForRm(work)) -= work }
    Try { backUpForRm -= work }

    Try { backUpPerGs(backUpForGs(work)) -= work }
    Try { backUpForGs -= work }
  }

  override def releaseBackUp(work: WorkRequest): Unit = ifOnline {
    logger.info(s"[GS\t${id}] Releasing back up for job ${work.job.id}")
    unregisterBackUp(work)
  }

  override def promote(req: PromoteRequest): Unit = ifOnline {
    logger.info(s"[GS\t${id}] Promoting to primary for job ${req.work.job.id}")
    unregisterBackUp(req.work)
    registerMonitor(req.work, req.rmId)
  }

  override def offLoad(req: OffLoadRequest): Unit = ifOnline {
    rmRepo.invokeOnEntity((rm, rmId) => {
      logger.info(s"[GS\t${id}] Offloading job ${req.work.job.id} to rm ${rmId}")
      rm.orderWork(WorkOrder(req.work, id))
    }, InvertedRandomWeighedSelector)
  }

  rmRepo.onOffline(rmId => {
    logger.info(s"[GS\t${id}] Recovering monitored jobs")

    synchronized {
      val jobsToReschedule = monitoringPerRm(rmId).clone()
      jobsToReschedule.foreach(work => {
        logger.info(s"[GS\t${id}] Rescheduling jobs ${work.job.id}")

        rmRepo.invokeOnEntity((rm, newRmId) => {
          unregisterMonitor(work)
          val workOrder = WorkOrder(work, newRmId)

          try {
            rm.orderWork(workOrder)
            registerMonitor(work, newRmId)
          } catch {
            case NonFatal(e) =>
          }

        }, InvertedRandomWeighedSelector, rmId)
      })
    }
  })

  gsRepo.onOffline(gsId => {
    logger.info(s"[GS\t${id}] Backup status check")
    synchronized {
      val backUpJobs = backUpPerGs(gsId).clone

      backUpJobs.foreach(work => {
        val rmId = backUpForRm(work)

        // check if rm is still running
        if(!rmRepo.checkStatus(rmId)) {
          // no -> monitor and reschedule, otherwise rm will take care of itself
          logger.info(s"[GS\t${id}] Use backup for job ${work.job.id}")
          val reschedule = rmRepo.invokeOnEntity((rm, newRmId) => {
            rm.orderWork(WorkOrder(work, newRmId))

            newRmId
          }, InvertedRandomWeighedSelector, rmId)

          reschedule match {
            case Some((_, newRmId)) => registerMonitor(work, newRmId)
            case _ => {}
          }

        }

      })
    }
  })

  @throws(classOf[RemoteException])
  override def ping(): Long = ifOnline { load }

  override def url: String = gsRepo.url(id)
}

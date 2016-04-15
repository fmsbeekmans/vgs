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
  }

  override def monitor(req: MonitorRequest): Unit = ifOnline {
    logger.info(s"[GS\t${id}] monitoring job ${req.work.job.id}")
    registerMonitor(req.work, req.rmId)
  }

  def registerMonitor(work: WorkRequest, rmId: Int): Unit = {
    monitoringForRm.put(work, rmId)
    monitoringPerRm(rmId) += work
  }

  def unregisterMonitor(work: WorkRequest): Unit = {
    monitoringPerRm(monitoringForRm(work)) - work
    monitoringForRm -= work
  }


  override def releaseMonitor(work: WorkRequest): Unit = ifOnline {
      logger.info(s"[GS\t${id}] releasing monitoring for job ${work.job.id}")
      unregisterMonitor(work)
  }

  override def backUp(req: BackUpRequest): Unit = ifOnline {
    logger.info(s"[GS\t${id}] backing up job ${req.work.job.id}")
    registerBackUp(req.work, req.rmId, req.monitorId)
  }

  def registerBackUp(work: WorkRequest, rmId: Int, monitorId: Int): Unit = {
    backUpForRm.put(work, rmId)
    backUpPerRm(rmId) += work

    backUpForGs.put(work, monitorId)
    backUpPerGs(monitorId) += work
  }

  def unregisterBackUp(work: WorkRequest): Unit = {
    backUpPerRm(backUpForRm(work)) -= work
    backUpForRm - work

    backUpPerGs(backUpForGs(work)) -= work
    backUpForGs - work
  }

  override def releaseBackUp(work: WorkRequest): Unit = ifOnline {
    logger.info(s"[GS\t${id}] releasing back up for job ${work.job.id}")
    unregisterBackUp(work)
  }

  override def promote(req: PromoteRequest): Unit = ifOnline {
    logger.info(s"[GS\t${id}] promoting to primary ${req.work.job.id}")
    unregisterBackUp(req.work)
    registerMonitor(req.work, req.rmId)
  }

  rmRepo.onOffline(rmId => {
    logger.info(s"[GS\t${id}] recovering monitored jobs")
    monitoringPerRm.synchronized(monitoringPerRm(rmId)).foreach(work => {
      rmRepo.invokeOnEntity((rm, newRmId) => {
        unregisterMonitor(work)
        val workOrder = WorkOrder(work, newRmId)

        try {
          rm.orderWork(workOrder)
        } catch {
          case NonFatal(e) =>
        }

      }, InvertedRandomWeighedSelector)
    })
  })

  @throws(classOf[RemoteException])
  override def ping(): Long = ifOnline { load }

  override def url: String = gsRepo.url(id)
}

package grid.gridScheduler

import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

import grid.discovery.{Pingable, RemoteShutDown, Repository}
import grid.messages.{BackUpRequest, MonitorRequest, PromoteRequest, WorkRequest}
import grid.resourceManager.IResourceManager
import grid.rmi.RmiServer

import collection.mutable._

class GridScheduler(val id: Int,
                    val rmRepo: Repository[IResourceManager],
                    val gsRepo: Repository[IGridScheduler]) extends UnicastRemoteObject with IGridScheduler with Pingable {

  RmiServer.register(this)

  var running = false

  var monitoringForRm: Map[WorkRequest, Int] = null
  var monitoringPerRm: Map[Int, Set[WorkRequest]] = null
  var backUpForRm: Map[WorkRequest, Int] = null
  var backUpPerRm: Map[Int, Set[WorkRequest]] = null
  var backUpForGs: Map[WorkRequest, Int] = null
  var backUpPerGs: Map[Int, Set[WorkRequest]] = null

  var load = 0

  start()

  def start(): Unit = {
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

    running = true
  }

  override def monitor(req: MonitorRequest): Unit = {
    println(s"[GS\t${id}] monitoring job ${req.work.job.id}")
    registerMonitor(req.work, req.rmId)
  }

  def registerMonitor(work: WorkRequest, rmId: Int): Unit = {
    monitoringForRm.put(work, rmId)
    monitoringPerRm(rmId) + work
  }

  def unregisterMonitor(work: WorkRequest): Unit = {
    monitoringPerRm(monitoringForRm(work)) - work
    monitoringForRm - work
  }


  override def releaseMonitor(work: WorkRequest): Unit = {
      println(s"[GS\t${id}] releasing monitoring for job ${work.job.id}")
      unregisterMonitor(work)
  }

  override def backUp(req: BackUpRequest): Unit = {
    println(s"[GS\t${id}] backing up job ${req.work.job.id}")
    registerBackUp(req.work, req.rmId, req.monitorId)
  }

  def registerBackUp(work: WorkRequest, rmId: Int, monitorId: Int): Unit = {
    backUpForRm.put(work, rmId)
    backUpPerRm(rmId) + work

    backUpForGs.put(work, monitorId)
    backUpPerGs(monitorId) + work
  }

  def unregisterBackUp(work: WorkRequest): Unit = {
    backUpPerRm(backUpForRm(work)) - work
    backUpForRm - work

    backUpPerGs(backUpForGs(work)) - work
    backUpForGs - work
  }

  override def releaseBackUp(work: WorkRequest): Unit = {
    println(s"[GS\t${id}] releasing back up for job ${work.job.id}")
    unregisterBackUp(work)
  }

  override def promote(req: PromoteRequest): Unit = {
    println(s"[GS\t${id}] promoting to primary ${req.work.job.id}")
    unregisterBackUp(req.work)
    registerMonitor(req.work, req.rmId)
  }


  @throws(classOf[RemoteException])
  override def ping(): Long = load

  override def url: String = gsRepo.url(id)
}

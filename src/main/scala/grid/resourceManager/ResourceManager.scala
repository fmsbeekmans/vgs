package grid.resourceManager

import collection.mutable._
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.{Executors, ScheduledExecutorService}

import com.typesafe.scalalogging.LazyLogging
import grid.cluster.{Pinger, RemoteShutDown}
import grid.discovery.Repository
import grid.discovery.Selector.{InvertedRandomWeighedSelector, WeighedRandomSelector}
import grid.gridScheduler.IGridScheduler
import grid.messages._
import grid.rmi.RmiServer
import grid.user.IUser

import scala.concurrent._
import scala.util.Try
import scala.util.control.Breaks._

class ResourceManager(val id: Int,
                      val n: Int,
                      val userRepo: Repository[IUser],
                      val rmRepo: Repository[IResourceManager],
                      val gsRepo: Repository[IGridScheduler]) extends UnicastRemoteObject with IResourceManager with RemoteShutDown with LazyLogging {

  var online: Boolean = false

  var timer: ScheduledExecutorService = null

  var pinger = new Pinger[IGridScheduler](gsRepo)

  var queue: Queue[WorkRequest] = null
  var idleNodes: Queue[Node] = null

  var monitor: Map[WorkRequest, Int] = null
  var monitoredBy: Map[Int, Set[WorkRequest]] = null
  var backUp: Map[WorkRequest, Int] = null
  var backedUpBy: Map[Int, Set[WorkRequest]] = null

  var load = 0

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutorService(
    Executors.newWorkStealingPool(128)
  )

  RmiServer.register(this)

  start()

  def start(): Unit = synchronized {
    timer = Executors.newScheduledThreadPool(8)

    queue = Queue()
    idleNodes = Queue()

    monitor = Map()
    monitoredBy = Map()
    backUp = Map()
    backedUpBy = Map()

    gsRepo.ids().foreach(gsId => {
      monitoredBy.put(gsId, Set())
      backedUpBy.put(gsId, Set())
    })

    val nodes = for {
      i <- 0 until n
      node = new Node(this, timer)
    } yield node

    nodes.foreach(idleNodes.enqueue(_))

    load = 0

    pinger.start()

    online = true

    logger.info(s"[RM\t${id}] Online")
  }

  def shutDown(): Unit = synchronized {
    timer.shutdownNow()

    queue = null
    idleNodes = null

    monitor = null
    monitoredBy = null
    backUp = null
    backedUpBy = null

    load = 0

    pinger.stop()

    online = false

    logger.info(s"[RM\t${id}] Offline")
  }

  @throws(classOf[RemoteException])
  override def orderWork(order: WorkOrder, ack: Boolean): Unit = ifOnline {
    val work = order.work
    val monitorId = order.monitorId
    registerMonitor(work, monitorId)
    logger.info(s"[RM\t${id}] Received work order for job ${work.job.id}")

    requestBackUp(work, monitorId) foreach {
      case None => synchronized {
        unregisterMonitor(work)
        unregisterBackUp(work)
      }
      case Some(_) => {
        if (online) {
          if (ack) {
            userRepo.getEntity(work.userId).foreach { u =>
              u.acceptJob(work.job)
            }
          }

          schedule(work)
        }
      }
    }
  }

  @throws(classOf[RemoteException])
  override def offerWork(work: WorkRequest): Unit = ifOnline {
    logger.info(s"[RM\t${id}] Received job ${work.job.id}")
    if(isFull) {
      offLoad(work)
    } else {
      requestMonitoringAndBackUp(work).foreach(_ =>
        if(online) {
          userRepo.getEntity(work.userId).foreach { u =>
            u.acceptJob(work.job)
          }

          Try { schedule(work) }
        }
      )
    }
  }

  def schedule(work: WorkRequest): Unit = ifOnline {

    logger.info(s"[RM\t${id}] Scheduling job ${work.job.id}")
    queue.synchronized(queue.enqueue(work))
    load += work.job.ms

    processQueue()
  }

  def isFull(): Boolean = synchronized {
    queue.length > n
  }

  def offLoad(work: WorkRequest): Unit = ifOnline {
    gsRepo.invokeOnEntity((gs, gsId) => {
      logger.info(s"[RM\t${id}] Offloading job ${work.job.id} through GS ${gsId}")
      gs.offLoad(OffLoadRequest(work))
    }, InvertedRandomWeighedSelector)
  }

  def requestMonitor(work: WorkRequest): Future[Option[Int]] = ifOnline {
    Future {
      blocking {
        gsRepo.invokeOnEntity((gs, gsId) => {
          gs.monitor(MonitorRequest(work, id))
        }, WeighedRandomSelector)
      } map { monitorId =>
        registerMonitor(work, monitorId)

        monitorId
      }
    }
  }

  def registerMonitor(work: WorkRequest, monitorId: Int) = synchronized {
    logger.info(s"[RM\t${id}] Job ${work.job.id} monitored at ${monitorId}")
    monitor.put(work, monitorId)
    monitoredBy(monitorId) += work
  }

  def unregisterMonitor(work: WorkRequest): Unit = synchronized {
    Try { monitoredBy(monitor(work)) -= work }
    Try { monitor -= work }
  }

  def requestBackUp(work: WorkRequest, monitorId: Int): Future[Option[Int]] = ifOnline {
    Future {
      blocking {
        gsRepo.invokeOnEntity((gs, gsId) => {
          gs.backUp(BackUpRequest(work, id, monitorId))
        }, WeighedRandomSelector, monitorId)
      } map { backUpId =>
        registerBackUp(work, backUpId)

        backUpId
      }
    }
  }

  def registerBackUp(work: WorkRequest, backUpId: Int) = synchronized {
    logger.info(s"[RM\t${id}] Job ${work.job.id} backed up at ${backUpId}")
    backUp.put(work, backUpId)
    backedUpBy(backUpId) += work
  }

  def unregisterBackUp(work: WorkRequest): Unit = synchronized {
    Try { backedUpBy(backUp(work)) -= work }
    Try { backUp -= work }
  }

  def requestMonitoringAndBackUp(work: WorkRequest): Future[Option[(Int, Int)]] = ifOnline {
    requestMonitor(work) flatMap {
      case Some(monitorId) if online => {
        requestBackUp(work, monitorId) map {
          case Some(backUpId) if online => Some((monitorId, backUpId))
          case _ => {
            unregisterMonitor(work)

            None
          }
        }
      }
      case _ => {
        Future { None }
      }
    }
  }

  @throws(classOf[RemoteException])
  override def finish(work: WorkRequest, node: Node): Unit = ifOnline {
    logger.info(s"[RM\t${id}] Finished executing job ${work.job.id}")
    synchronized(load -= work.job.ms)
    idleNodes.synchronized(idleNodes.enqueue(node))

    logger.info(s"[RM\t${id}] Releasing job ${work.job.id}")

    val releases = for {
      acceptResult <- Future {
        blocking {
          userRepo.getEntity(work.userId).foreach(_.acceptResult(work.job))
        }
      }
      releaseMonitor <- Future {
        blocking {
          gsRepo.getEntity(monitor(work)).foreach(gs => gs.releaseMonitor(work))
          unregisterMonitor(work)
        }
      }
      backUpMonitor <- Future {
        blocking {
          gsRepo.getEntity(backUp(work)).foreach(gs => gs.releaseBackUp(work))
          unregisterBackUp(work)
        }
      }
    } yield (acceptResult, releaseMonitor, backUpMonitor)

    processQueue()
  }

  def processQueue(): Unit = ifOnline {
    breakable {

    var work: Option[WorkRequest] = None
    var worker: Option[Node] = None

    if(!online) break

      worker = Try { idleNodes.synchronized { idleNodes.dequeue() } }.toOption
      work = Try { queue.synchronized { queue.dequeue() } }.toOption

      (worker, work) match {
        case (None, None) => break
        case (None, Some(work)) => queue.synchronized(queue.enqueue(work)); break
        case (Some(worker), None) => idleNodes.synchronized(idleNodes.enqueue(worker)); break
        case (Some(worker), Some(work)) => {
          logger.info(s"[RM\t${id}] Executing job ${work.job.id}")
          worker.handle(work)
        }
      }
    }
  }

  gsRepo.onOffline(gsId => {
    // restoring monitors
    val restoreMonitor = monitoredBy(gsId).clone()
    restoreMonitor.foreach(work => {
      logger.info(s"[RM\t${id}] Recovering from monitor crash for job ${work.job.id}")
      val backUpId = backUp(work)

      val monitorId = monitor(work)
      // try to promote backup
      gsRepo.getEntity(backUpId).map(monitor =>
        monitor.promote(PromoteRequest(work, id))
      ).foreach { _ =>
        unregisterBackUp(work)
        gsRepo.getEntity(backUpId).foreach { backUp => backUp.releaseBackUp(work) }
        registerMonitor(work, backUpId)
        requestBackUp(work, backUpId)
      }
    })

    // Restoring backups
    val restoreBackUp = backedUpBy(gsId).clone()

    restoreBackUp.foreach { work =>
      logger.info(s"[RM\t${id}] Recovering from back-up crash for job ${work.job.id}")
      val monitorId = monitor(work)

      // does the monitor still respond?
      if(gsRepo.checkStatus(monitorId)) {
        // yes -> new backup
        unregisterBackUp(work)
        requestBackUp(work, monitorId)
      } else {
        unregisterBackUp(work)
        unregisterMonitor(work)
        requestMonitoringAndBackUp(work)
      }
    }
  })

  @throws(classOf[RemoteException])
  override def ping(): Long = ifOnline {
    load
  }

  override def url: String = rmRepo.url(id)
}
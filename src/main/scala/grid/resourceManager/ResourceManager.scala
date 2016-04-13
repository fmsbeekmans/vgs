package grid.resourceManager

import collection.mutable._
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.{Executors, ScheduledExecutorService}

import grid.discovery.{Pingable, Repository}
import grid.discovery.Selector.WeighedRandomSelector
import grid.gridScheduler.IGridScheduler
import grid.messages.{BackUpRequest, MonitorRequest, WorkOrder, WorkRequest}
import grid.rmi.RmiServer
import grid.user.IUser

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class ResourceManager(val id: Int,
                      val n: Int,
                      val userRepo: Repository[IUser],
                      val rmRepo: Repository[IResourceManager],
                      val gsRepo: Repository[IGridScheduler]) extends UnicastRemoteObject with IResourceManager with Pingable {

  var timer: ScheduledExecutorService = null

  var running: Boolean = false

  var jobLocks: Map[WorkRequest, Object] = null
  var queue: Queue[WorkRequest] = null
  var idleNodes: Queue[Node] = null

  var monitor: Map[WorkRequest, Int] = null
  var monitoredBy: Map[Int, Set[WorkRequest]] = null
  var backUp: Map[WorkRequest, Int] = null
  var backedUpBy: Map[Int, Set[WorkRequest]] = null

  var load = 0

  RmiServer.register(this)

  start()

  def start(): Unit = synchronized {
    timer = Executors.newSingleThreadScheduledExecutor()

    jobLocks = Map()
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

    running = true
  }

  @throws(classOf[RemoteException])
  override def orderWork(req: WorkOrder): Unit =  {
    jobLocks(req.work).synchronized {
      requestBackUp(req.work, req.monitorId) match {
        case Some(_) => {
          registerMonitor(req.work, req.monitorId)
          queue.synchronized(queue.enqueue(req.work))
          processQueue()
        }
        case None => {
          unregisterBackUp(req.work)
        }
      }
    }
  }

  @throws(classOf[RemoteException])
  override def offerWork(work: WorkRequest): Unit = {
    println(s"[RM\t${id}] received job ${work.job.id}")
    val lock: WorkRequest = work.copy()
    jobLocks.put(lock, new Object)
    lock.synchronized {
      queue.synchronized {
        Future {
          requestMonitor(work).flatMap(monitorId => {
            requestBackUp(work, monitorId)
          }) match {
            case Some(_) => {
              queue.enqueue(work)
              processQueue()
            }
            case None => {
              unregisterMonitor(work)
              unregisterBackUp(work)
            }
          }
        }
      }
    }
  }

  def requestMonitor(work: WorkRequest): Option[Int] = {
    val result = gsRepo.invokeOnEntity((gs, gsId) => {
      gs.monitor(MonitorRequest(work, id))
    }, WeighedRandomSelector)

  if(result.isDefined) registerMonitor(work, result.get._2)

    result.map(_._2)
  }

  def registerMonitor(work: WorkRequest, monitorId: Int) = {
    jobLocks(work).synchronized {
      monitor.put(work, monitorId)
      monitoredBy(monitorId) + work
    }
  }

  def unregisterMonitor(work: WorkRequest): Unit = {
    jobLocks(work).synchronized {
      monitoredBy(monitor(work)) - work
      monitor - work
    }
  }

  def requestBackUp(work: WorkRequest, monitorId: Int): Option[Int] = {
    val result = gsRepo.invokeOnEntity((gs, gsId) => {
      gs.backUp(BackUpRequest(work, id, monitorId))
    }, WeighedRandomSelector, monitorId)

    if(result.isDefined) registerBackUp(work, result.get._2)

    result.map(_._2)
  }

  def registerBackUp(work: WorkRequest, backUpId: Int) = {
    jobLocks(work).synchronized {
      backUp.put(work, backUpId)
      backedUpBy(backUpId) + work
    }
  }

  def unregisterBackUp(work: WorkRequest): Unit = {
    jobLocks(work).synchronized {
      backedUpBy(backUp(work)) - work
      backUp - work
    }
  }

  @throws(classOf[RemoteException])
  override def finish(work: WorkRequest, node: Node): Unit = {
    jobLocks(work).synchronized {
      println(s"[RM\t${id}] finished executing job ${work.job.id}")
      userRepo.getEntity(work.userId).foreach(u => {
        u.acceptResult(work.job)
        release(work)
        idleNodes.synchronized(idleNodes.enqueue(node))
      })
    }

    processQueue()
  }

  def release(work: WorkRequest): Unit = {
    jobLocks(work).synchronized {
      println(s"[RM\t${id}] releasing job ${work.job.id}")
      Try { gsRepo.getEntity(monitor(work)).foreach(gs => gs.releaseMonitor(work)) }
      Try { gsRepo.getEntity(backUp(work)).foreach(gs => gs.releaseBackUp(work)) }
    }

    jobLocks - work
  }

  def processQueue(): Unit = {
    queue.synchronized {
      idleNodes.synchronized {
        while(queue.nonEmpty && idleNodes.nonEmpty) {
          val work = queue.dequeue()
          val worker = idleNodes.dequeue()

          println(s"[RM\t${id}] starting job ${work.job.id}")

          Future { worker.handle(work) }
        }
      }
    }
  }

  @throws(classOf[RemoteException])
  override def ping(): Long = load

  override def url: String = rmRepo.url(id)
}

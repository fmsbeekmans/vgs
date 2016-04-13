package grid.resourceManager

import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.{Executors, ScheduledExecutorService}

import grid.discovery.Repository
import grid.discovery.Selector.WeighedRandomSelector
import grid.gridScheduler.IGridScheduler
import grid.messages.{BackUpRequest, MonitorRequest, WorkOrder, WorkRequest}
import grid.rmi.RmiServer
import grid.user.IUser

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ResourceManager(val id: Int,
                      val n: Int,
                      val userRepo: Repository[IUser],
                      val rmRepo: Repository[IResourceManager],
                      val gsRepo: Repository[IGridScheduler]) extends UnicastRemoteObject with IResourceManager {

  import collection._

  var timer: ScheduledExecutorService = null

  var running: Boolean = false

  var jobLocks: mutable.Map[WorkRequest, Object] = null
  var queue: mutable.Queue[WorkRequest] = null
  var idleNodes: mutable.Queue[Node] = null

  var monitor: mutable.Map[WorkRequest, Int] = null
  var monitoredBy: mutable.Map[Int, Set[WorkRequest]] = null
  var backUp: mutable.Map[WorkRequest, Int] = null
  var backedUpBy: mutable.Map[Int, Set[WorkRequest]] = null

  RmiServer.register(this)

  start()

  def start(): Unit = synchronized {
    timer = Executors.newSingleThreadScheduledExecutor()

    jobLocks = mutable.Map()
    queue = mutable.Queue()
    idleNodes = mutable.Queue()

    monitor = mutable.Map()
    monitoredBy = mutable.Map()
    backUp = mutable.Map()
    backedUpBy = mutable.Map()

    gsRepo.ids().foreach(gsId => {
      monitoredBy.put(gsId, mutable.Set())
      backedUpBy.put(gsId, mutable.Set())
    })

    val nodes = for {
      i <- 0 until n
      node = new Node(this, timer)
    } yield node

    nodes.foreach(idleNodes.enqueue(_))

    running = true
  }

  @throws(classOf[RemoteException])
  override def orderWork(req: WorkOrder): Unit =  {

  }

  @throws(classOf[RemoteException])
  override def offerWork(work: WorkRequest): Unit = {
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
              removeMonitorAdmin(work)
              removeBackUpAdmin(work)
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

  def removeMonitorAdmin(work: WorkRequest): Unit = {
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

  def removeBackUpAdmin(work: WorkRequest): Unit = {
    jobLocks(work).synchronized {
      backedUpBy(backUp(work)) - work
      backUp - work
    }
  }

  @throws(classOf[RemoteException])
  override def finish(work: WorkRequest, node: Node): Unit = {
    jobLocks(work).synchronized {
      userRepo.getEntity(work.userId).foreach(u => {
        u.acceptResult(work.job)
        release(work)
        idleNodes.synchronized(idleNodes.enqueue(node))
      })
    }
  }

  def release(work: WorkRequest): Unit = {
    jobLocks(work).synchronized {

    }
  }

  def processQueue(): Unit = {
    queue.synchronized {
      idleNodes.synchronized {
        while(queue.nonEmpty && idleNodes.nonEmpty) {
          val work = queue.dequeue()
          val worker = idleNodes.dequeue()

          Future {
            worker.handle(work)
          }
        }
      }
    }
  }

  override def url: String = rmRepo.url(id)
}

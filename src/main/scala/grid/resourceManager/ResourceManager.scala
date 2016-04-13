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

import scala.concurrent._
import scala.util.Try

class ResourceManager(val id: Int,
                      val n: Int,
                      val userRepo: Repository[IUser],
                      val rmRepo: Repository[IResourceManager],
                      val gsRepo: Repository[IGridScheduler]) extends UnicastRemoteObject with IResourceManager with Pingable {

  var timer: ScheduledExecutorService = null

  var running: Boolean = false

  var queue: Queue[WorkRequest] = null
  var idleNodes: Queue[Node] = null

  var monitor: Map[WorkRequest, Int] = null
  var monitoredBy: Map[Int, Set[WorkRequest]] = null
  var backUp: Map[WorkRequest, Int] = null
  var backedUpBy: Map[Int, Set[WorkRequest]] = null

  var load = 0

  val receiveWorkExecutor: ExecutionContext = ExecutionContext.fromExecutorService(
    Executors.newWorkStealingPool(4)
  )
  val queueExecutor: ExecutionContext = ExecutionContext.fromExecutorService(
      Executors.newWorkStealingPool(4)
  )
  val gsExecutor: ExecutionContext = ExecutionContext.fromExecutorService(
    Executors.newWorkStealingPool(4)
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

    running = true
  }

  @throws(classOf[RemoteException])
  override def orderWork(req: WorkOrder): Unit =  {
    Future {
      blocking {
        requestBackUp(req.work, req.monitorId) match {
          case Some(_) => {
            registerMonitor(req.work, req.monitorId)
            queue.enqueue(req.work)
            processQueue()
          }
          case None => {
            unregisterBackUp(req.work)
          }
        }
      }
    }(receiveWorkExecutor)
  }

  @throws(classOf[RemoteException])
  override def offerWork(work: WorkRequest): Unit = {
    println(s"[RM\t${id}] received job ${work.job.id}")
    Future {
      blocking {
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
    }(receiveWorkExecutor)
  }

  def requestMonitor(work: WorkRequest): Option[Int] = {
    val result = gsRepo.invokeOnEntity((gs, gsId) => {
      gs.monitor(MonitorRequest(work, id))
    }, WeighedRandomSelector)

  if(result.isDefined) registerMonitor(work, result.get._2)

    result.map(_._2)
  }

  def registerMonitor(work: WorkRequest, monitorId: Int) = {
    monitor.put(work, monitorId)
    monitoredBy(monitorId) + work
  }

  def unregisterMonitor(work: WorkRequest): Unit = {
    monitoredBy(monitor(work)) - work
    monitor - work
  }

  def requestBackUp(work: WorkRequest, monitorId: Int): Option[Int] = {
    val result = gsRepo.invokeOnEntity((gs, gsId) => {
      gs.backUp(BackUpRequest(work, id, monitorId))
    }, WeighedRandomSelector, monitorId)

    if(result.isDefined) registerBackUp(work, result.get._2)

    result.map(_._2)
  }

  def registerBackUp(work: WorkRequest, backUpId: Int) = {
    backUp.put(work, backUpId)
    backedUpBy(backUpId) + work
  }

  def unregisterBackUp(work: WorkRequest): Unit = {
    backedUpBy(backUp(work)) - work
    backUp - work
  }

  @throws(classOf[RemoteException])
  override def finish(work: WorkRequest, node: Node): Unit = {
    println(s"[RM\t${id}] finished executing job ${work.job.id}")
    userRepo.getEntity(work.userId).foreach(u => {
      u.acceptResult(work.job)

      idleNodes.enqueue(node)

      Future {
        blocking {
          println(s"[RM\t${id}] releasing job ${work.job.id}")
          Try {
            gsRepo.getEntity(monitor(work)).foreach(gs => gs.releaseMonitor(work))
            gsRepo.getEntity(backUp(work)).foreach(gs => gs.releaseBackUp(work))
          }
        }
      }(gsExecutor).map((release: Try[Unit]) => processQueue())(gsExecutor)
    })

  }

  def processQueue(): Unit = {
    while(queue.nonEmpty && idleNodes.nonEmpty) {
      val work = queue.dequeue()
      val worker = idleNodes.dequeue()

      println(s"[RM\t${id}] starting job ${work.job.id}")

      Future {
        blocking {
          worker.handle(work)
        }
      }(queueExecutor)
    }
  }

  @throws(classOf[RemoteException])
  override def ping(): Long = load

  override def url: String = rmRepo.url(id)
}

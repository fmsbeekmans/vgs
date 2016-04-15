package grid.cluster

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import grid.discovery.Repository

import scala.util.{Failure, Success, Try}

class Pinger[T <: Addressable](repo: Repository[T]) {

  var timer: ScheduledExecutorService = null

  def start(): Unit = {
    timer = Executors.newSingleThreadScheduledExecutor()

    timer.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = repo.ids().foreach(id => {
        repo.getEntity(id).foreach(entity => Try {
          entity.ping()
        } match {
          case Success(ping) => {
            repo.load.put(id, ping)
            repo.setStatus(id, true)
          }
          case Failure(_) => {
            repo.load - id
            repo.setStatus(id, false)
          }
        })
      })
    }, 100, 100, TimeUnit.MILLISECONDS)
  }

  def stop(): Unit = {
    timer.shutdownNow()
  }
}

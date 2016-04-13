package grid.discovery

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import grid.rmi.Addressable

import scala.util.{Failure, Success, Try}

class Pinger[T <: Pingable](repo: Repository[T]) {

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
            repo.isOnline.put(id, true)
          }
          case Failure(_) => {
            repo.load - id
            repo.isOnline.put(id, false)
          }
        })
      })
    }, 100, 100, TimeUnit.MILLISECONDS)
  }

  def stop(): Unit = {

  }
}

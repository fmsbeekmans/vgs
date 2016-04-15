package grid.cluster

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import scala.annotation.tailrec
import scala.collection.mutable._
import scala.util.Random

class CrashSimulator(val meanTimeToCrash: Int, val meanCrashDuration: Int) {
  val timer: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

  def simulateCrashes(canCrash: RemoteShutDown): Unit = {
    timer.schedule(new Runnable {
      override def run(): Unit = {
        canCrash.shutDown()
        timer.schedule(new Runnable {
          override def run(): Unit = {
            canCrash.start()

            simulateCrashes(canCrash)
          }
        }, Random.nextInt(meanCrashDuration * 2), TimeUnit.MILLISECONDS)
      }
    }, Random.nextInt(meanTimeToCrash * 2), TimeUnit.MILLISECONDS)
  }
}

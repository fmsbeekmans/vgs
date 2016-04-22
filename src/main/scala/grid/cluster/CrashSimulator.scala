package grid.cluster

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import scala.util.Random

class CrashSimulator(val minTimeToCrash: Int, val maxTimeToCrash: Int, val minCrashDuration: Int, val maxCrashDuration: Int) {
  val timer: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
  val dTimeToCrash = maxTimeToCrash - minTimeToCrash
  val dDuration = maxCrashDuration - minCrashDuration

  def simulateCrashes(canCrash: RemoteShutDown): Unit = {
    timer.schedule(new Runnable {
      override def run(): Unit = {
        canCrash.shutDown()
        timer.schedule(new Runnable {
          override def run(): Unit = {
            canCrash.start()

            simulateCrashes(canCrash)
          }
        }, minCrashDuration + Random.nextInt(dDuration * 2), TimeUnit.MILLISECONDS)
      }
    }, minTimeToCrash + Random.nextInt(dTimeToCrash * 2), TimeUnit.MILLISECONDS)
  }
}
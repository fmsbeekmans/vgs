package grid.user

import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import grid.discovery.Repository
import grid.resourceManager.IResourceManager

import scala.concurrent.Promise
import scala.util.Success

import scala.util.control.Breaks._

class WorkLoadGenerator(val bursts: Int,
                        val burstSize: Int,
                        val meanJobTime: Int,
                        val dt: Int,
                        val weights: Map[Int, Int],
                        val rmRepo: Repository[IResourceManager],
                        val timer: ScheduledExecutorService) {
  def createLoad(forUser: IUser): Promise[Unit] = {
    val promise = Promise[Unit]()

    for {
      i <- 0 until bursts
      t = i * dt
    } {
      timer.schedule(new Runnable {
        override def run(): Unit = {
          createLoad(forUser, burstSize, meanJobTime, weights)
          if(i == bursts - 1) {
            promise.complete(Success())
          }
        }
      }, t, TimeUnit.MILLISECONDS)
    }

    promise
  }

  def createLoad(forUser: IUser, burstSize: Int, meanJobTime: Int, weights: Map[Int, Int]): Unit = {
    val n = weights.values.sum
    weights.foreach {
      case (rmId, weight) => {
        val jobs = Math.ceil(weight/n*burstSize).toInt
        println(s"Jobs: ${jobs}")
        forUser.createJobs(rmId, jobs, meanJobTime)
      }
    }
  }
}

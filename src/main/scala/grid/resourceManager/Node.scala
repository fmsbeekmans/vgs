package grid.resourceManager

import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import grid.messages.WorkRequest
import grid.model.Job

class Node(val rm: IResourceManager, val timer: ScheduledExecutorService) { self =>
  def handle(work: WorkRequest): Unit = {
    timer.schedule(new Runnable {
      override def run(): Unit = {
        rm.finish(work, self)
      }
    }, work.job.ms, TimeUnit.MILLISECONDS)
  }
}

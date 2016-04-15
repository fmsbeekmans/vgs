package grid

import grid.discovery.Repository
import grid.gridScheduler.GridScheduler
import grid.resourceManager.ResourceManager
import grid.user.User

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RunJob extends App {

  import Repository._

  val gs0 = new GridScheduler(
    0,
    rmRepo,
    gsRepo
  )

  val gs1 = new GridScheduler(
    1,
    rmRepo,
    gsRepo
  )

  val rm0 = new ResourceManager(
    0,
    10000,
    userRepo,
    rmRepo,
    gsRepo
  )

  val rm1 = new ResourceManager(
    1,
    10000,
    userRepo,
    rmRepo,
    gsRepo
  )

  val user = new User(
    0,
    userRepo,
    rmRepo
  )

  Future { user.createJobs(0, 10000, 10000) }

  Thread.sleep(8000)

  rm0.shutDown()
}

package grid

import java.nio.file.{Path, Paths}

import grid.discovery.Repository
import grid.gridScheduler.{GridScheduler, IGridScheduler}
import grid.resourceManager.{IResourceManager, ResourceManager}
import grid.user.{IUser, User}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RunJob extends App {
  val gsRepo = Repository.fromFile[IGridScheduler](resourcePath("gss"))
  val rmRepo = Repository.fromFile[IResourceManager](resourcePath("rms"))
  val userRepo = Repository.fromFile[IUser](resourcePath("users"))

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
    1000000,
    userRepo,
    rmRepo,
    gsRepo
  )

  val rm1 = new ResourceManager(
    1,
    1000000,
    userRepo,
    rmRepo,
    gsRepo
  )

  val user = new User(
    0,
    userRepo,
    rmRepo
  )

  Future { user.createJobs(0, 100, 1000) }

  Thread.sleep(300)

  rm0.shutDown()

  def resourcePath(fileName: String): Path = {
    val url = getClass.getClassLoader.getResource(fileName)
    Paths.get(url.toURI)
  }
}

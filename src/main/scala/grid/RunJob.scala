package grid

import java.nio.file.{Path, Paths}

import grid.discovery.Repository
import grid.gridScheduler.{GridScheduler, IGridScheduler}
import grid.resourceManager.{IResourceManager, ResourceManager}
import grid.user.{IUser, User}

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

  val rm = new ResourceManager(
    0,
    10,
    userRepo,
    rmRepo,
    gsRepo
  )

  val user = new User(
    0,
    userRepo,
    rmRepo
  )

  user.createJobs(0, 20000, 10)

  def resourcePath(fileName: String): Path = {
    val url = getClass.getClassLoader.getResource(fileName)
    Paths.get(url.toURI)
  }
}

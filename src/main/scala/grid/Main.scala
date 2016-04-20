package grid

import java.util.concurrent.Executors

import grid.cluster.CrashSimulator
import grid.discovery.Repository
import grid.gridScheduler.GridScheduler
import grid.resourceManager.ResourceManager
import grid.user.User

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  import Repository._

  def main(args: Array[String]) {

    val url: String = args(0)
    val manifest = Manifest.fromFileName(url)
    val repoPath = manifest.repositoryPath

    val timer = Executors.newScheduledThreadPool(4)
//    val gsCrasher = new CrashSimulator(manifest.meanTimeToGsCrash, manifest.meanGsCrashDuration, timer)
//    val rmCrasher = new CrashSimulator(manifest.meanTimeToRmCrash, manifest.meanRmCrashDuration, timer)

    manifest.gsIds.foreach(gsId => {
      val gs = new GridScheduler(
        gsId,
        rmRepo(repoPath + "/rms"),
        gsRepo(repoPath + "/gss")
      )
    })

    manifest.rmIds.foreach(rmId => {
      val rm = new ResourceManager(
        rmId,
        manifest.nodesPerRm,
        userRepo(repoPath + "/users"),
        rmRepo(repoPath + "/rms"),
        gsRepo(repoPath + "/gss")
      )
    })

    manifest.userIds.foreach(userId => {
      val user = new User(
        userId,
        userRepo(repoPath + "/users"),
        rmRepo(repoPath + "/rms")
      )

      Future {
        user.createJobs(userId, 5000, 3000)
      }
    })
  }
}

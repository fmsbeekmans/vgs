package grid.user

import java.rmi.RemoteException

import grid.model.Job
import grid.rmi.Addressable

import scala.concurrent.duration.Duration

trait IUser extends Addressable {

  @throws(classOf[RemoteException])
  def createJobs(rmId: Int, n: Int, ms: Int): Unit

  @throws(classOf[RemoteException])
  def acceptResult(job: Job): Unit
}

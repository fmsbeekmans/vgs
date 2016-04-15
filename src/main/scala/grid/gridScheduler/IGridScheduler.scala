package grid.gridScheduler

import java.rmi.RemoteException

import grid.cluster.{Addressable, RemoteShutDown}
import grid.messages._

trait IGridScheduler extends Addressable with RemoteShutDown {

  @throws(classOf[RemoteException])
  def monitor(req: MonitorRequest): Unit

  @throws(classOf[RemoteException])
  def backUp(req: BackUpRequest): Unit

  @throws(classOf[RemoteException])
  def promote(req: PromoteRequest): Unit

  @throws(classOf[RemoteException])
  def releaseMonitor(req: WorkRequest): Unit

  @throws(classOf[RemoteException])
  def releaseBackUp(req: WorkRequest): Unit
}

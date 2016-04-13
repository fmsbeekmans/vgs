package grid.gridScheduler

import java.rmi.RemoteException

import grid.messages._
import grid.rmi.Addressable

trait IGridScheduler extends Addressable {

  @throws(classOf[RemoteException])
  def monitor(req: MonitorRequest): Unit

  @throws(classOf[RemoteException])
  def backUp(req: BackUpRequest): Unit

  @throws(classOf[RemoteException])
  def promote(req: PromoteRequest): Unit
}

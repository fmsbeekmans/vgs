package grid.resourceManager

import java.rmi.RemoteException

import grid.messages._
import grid.model.Job
import grid.rmi.Addressable

trait IResourceManager extends Addressable {

  @throws(classOf[RemoteException])
  def offerWork(work: WorkRequest) {}

  @throws(classOf[RemoteException])
  def orderWork(work: WorkOrder)

  @throws(classOf[RemoteException])
  def finish(work: WorkRequest, worker: Node)

}

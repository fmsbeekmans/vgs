package grid.resourceManager

import java.rmi.RemoteException

import grid.cluster.{Addressable, RemoteShutDown}
import grid.messages._
import grid.model.Job

trait IResourceManager extends Addressable with RemoteShutDown {

  @throws(classOf[RemoteException])
  def offerWork(work: WorkRequest) {}

  @throws(classOf[RemoteException])
  def orderWork(work: WorkOrder)

  @throws(classOf[RemoteException])
  def finish(work: WorkRequest, worker: Node)

}

package grid.messages

import java.rmi.RemoteException

import grid.model.Job
import grid.user.IUser

@SerialVersionUID(1L)
case class WorkRequest(job: Job, userId: Int) extends Serializable

@SerialVersionUID(2L)
case class WorkOrder(work: WorkRequest, monitorId: Int) extends Serializable

@SerialVersionUID(3L)
case class MonitorRequest(work: WorkRequest, rmId: Int) extends Serializable

@SerialVersionUID(4L)
case class BackUpRequest(work: WorkRequest, rmId: Int, monitorId: Int) extends Serializable

@SerialVersionUID(5L)
case class PromoteRequest(work: WorkRequest, rmId: Int) extends Serializable


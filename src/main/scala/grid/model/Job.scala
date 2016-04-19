package grid.model

@SerialVersionUID(0L)
case class Job(id: Int, firstRmId: Int, ms: Int, otherRms: Seq[Int] = List()) extends Serializable {
  val created = System.currentTimeMillis()
  def hop(rmId: Int) = {
    copy(otherRms = otherRms :+ rmId)
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case Job(id, rmId, _, _) => id == this.id && rmId == firstRmId
    }
  }
}

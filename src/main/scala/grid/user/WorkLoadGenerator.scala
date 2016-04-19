package grid.user

class WorkLoadGenerator(val bursts: Int, val dt: Int, val weights: Map[Int, Int]) {
  def createLoad(forUser: IUser)
}

package grid.discovery.Selector

trait Selector {
  def selectIndex(weights: Map[Int, Long]): Option[Int]
}

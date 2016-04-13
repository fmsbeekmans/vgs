package grid.discovery.Selector

object InvertedRandomWeighedSelector extends Selector {
  override def selectIndex(weights: Map[Int, Long]): Option[Int] = {
    val max = weights.values.max

    WeighedRandomSelector.selectIndex(weights.mapValues(max - _))
  }
}

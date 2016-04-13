package grid.discovery.Selector

import scala.util.Random
import scala.util.control.Breaks._

object WeighedRandomSelector extends Selector {
  val rng: Random = new Random()

  override def selectIndex(weights: Map[Int, Long]): Option[Int] = {
    if(weights.isEmpty) {
      None
    } else {

      val sum = weights.values.sum + weights.size

      var randomResult: Long = -1
      while (randomResult == -1) {
        import math._
        val randomTry = floor(rng.nextDouble() * sum).toLong

        if (randomTry <= sum) randomResult = randomTry
      }

      var index: Option[Int] = None
      var counter = 0L
      val keys = weights.keys.toList
      var cursor = 0

      breakable {
        while(cursor < keys.length) {
          if (counter <= randomResult && (counter + weights(keys(cursor)) + 1) > randomResult) {
            index = Some(keys(cursor))
            break
          }
          counter += weights(keys(cursor)) + 1
          cursor += 1
        }
      }

      index
    }
  }
}

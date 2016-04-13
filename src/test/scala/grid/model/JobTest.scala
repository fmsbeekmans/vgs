package grid.model

import org.scalatest._

class JobTest extends FlatSpec with Matchers with GivenWhenThen {
  "A job" should "should track it's hops" in {
    val start = Job(0, 0, 100)
    When("Job moves an RM")
    val hop = start.hop(1).hop(2)
    Then("It should be tracked")
    hop.otherRms shouldBe(Seq(1, 2))
  }

  "Jobs are with different paths" should "be equal" in {
    Job(1, 2, 100) shouldBe Job(1, 2, 100).hop(3)
  }
}

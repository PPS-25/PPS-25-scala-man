package it.unibo.pps.scalaman.model.score

import it.unibo.pps.scalaman.model.score.ScoringEvent.{
  BasicItem,
  BonusItem,
  EnemyKill,
  RemainingLives
}
import org.scalatest.funsuite.AnyFunSuite

class ScoreTrackerTest extends AnyFunSuite:

  private val emptyTracker = ScoreTracker()

  test("a new tracker starts with 0 points and a combo of 0") {
    assert(emptyTracker.currentScore == 0)
    assert(emptyTracker.combo == 0)
  }

  test("every event awards the points defined by its scoring rule") {
    val expectedPoints = Seq(
      BasicItem -> 50,
      BonusItem -> 100,
      RemainingLives(0) -> 0,
      RemainingLives(1) -> 500,
      RemainingLives(3) -> 1500
    )
    expectedPoints.foreach { case (event, points) =>
      assert(summon[ScoringRule].awardedPoints(event) == points)
    }
  }

  test("a score increase adds the points of the event") {
    assert(emptyTracker.increaseScore(BasicItem).currentScore == 50)
  }

  test("a score increase accumulates points") {
    assert(
      emptyTracker
        .increaseScore(BasicItem)
        .increaseScore(BasicItem)
        .increaseScore(EnemyKill)
        .currentScore == 50 + 50 + 200
    )
  }

  test("an enemy kill awards 200 points, doubled at every combo increase") {
    val expectedPoints = Seq(
      1 -> 200,
      2 -> 400,
      3 -> 800,
      4 -> 1600
    )
    expectedPoints.foreach { case (combo, points) =>
      assert(summon[ScoringRule].awardedPoints(EnemyKill, combo) == points)
    }
  }

  test("resetting a combo brings its total back to 0") {
    assert(ScoreTracker(combo = 3).resetCombo.combo == 0)
  }

  test("resetting a combo does not reset points") {
    assert(
      ScoreTracker(combo = 3)
        .increaseScore(BonusItem)
        .resetCombo
        .currentScore > 0
    )
  }

  test("increasing points does not reset a combo") {
    assert(
      ScoreTracker(combo = 3)
        .increaseScore(BonusItem)
        .combo == 3
    )
  }

  test("resetting the combo brings the awarded points for the next kill back to 200") {
    assert(
      ScoreTracker(combo = 3).resetCombo
        .increaseScore(EnemyKill)
        .currentScore == 200
    )
  }

  test("the game result keeps the name of the player") {
    assert(
      emptyTracker
        .toResult("PlayerName", 0)
        .playerName == "PlayerName"
    )
  }

  test("the game result considers the remaining lives to increase the final score") {
    assert(
      emptyTracker
        .toResult("PlayerName", 2)
        .score == 500 + 500
    )
  }

  test("the game result keeps the score of the game") {
    assert(
      ScoreTracker(500, 0)
        .toResult("PlayerName", 0)
        .score == 500
    )
  }

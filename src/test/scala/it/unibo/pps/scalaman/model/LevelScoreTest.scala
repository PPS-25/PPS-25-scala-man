package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.LevelTestSupport.{item, levelWith, startingLevel}
import it.unibo.pps.scalaman.model.collectibles.Collectibles
import it.unibo.pps.scalaman.model.score.ScoreTracker
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class LevelScoreTest extends AnyFunSuite:
  private val won = startingLevel.copy(collectibles = Collectibles(Set.empty))
  private def timedAt(elapsed: Int) =
    startingLevel.copy(mode = GameMode.Timed(60.seconds), clock = GameClock(elapsed.seconds))

  test("a game being played is worth what it has scored so far") {
    assert(startingLevel.copy(score = ScoreTracker(300)).liveScore == 300)
  }

  test("a classic game that ended counts the lives it had left") {
    assert(won.copy(score = ScoreTracker(300), progress = LevelProgress(2)).liveScore == 300 + 1000)
  }

  test("a classic game does not count its lives before it is over") {
    assert(startingLevel.copy(progress = LevelProgress(2)).liveScore == 0)
  }

  test("a game still being played has no result") {
    assert(startingLevel.result("PlayerName").isEmpty)
  }

  test("the result keeps the name of whoever played") {
    assert(won.result("PlayerName").map(_.playerName).contains("PlayerName"))
  }

  test("the result is worth what the game is worth") {
    val ended = won.copy(score = ScoreTracker(300), progress = LevelProgress(2))
    assert(ended.result("PlayerName").map(_.score).contains(ended.liveScore))
  }

  test("a timed game is worth the seconds it has left") {
    assert(timedAt(20).liveScore == 40)
  }

  test("a timed game that ran out of time is worth nothing") {
    assert(timedAt(60).status == GameState.Defeat)
    assert(timedAt(60).liveScore == 0)
  }

  test("a survival game is worth the waves it has survived") {
    val survival = startingLevel.copy(
      mode = GameMode.Survival(difficultyEvery = 10.seconds),
      clock = GameClock(35.seconds)
    )
    assert(survival.liveScore == 3)
  }

  test("collecting is worth points in a classic game") {
    assert(levelWith(item.position).collecting.score.currentScore == 50)
  }

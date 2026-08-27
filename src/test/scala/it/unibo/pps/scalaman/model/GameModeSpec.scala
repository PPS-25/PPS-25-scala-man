package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.GameState.{Defeat, Running, Victory}
import it.unibo.pps.scalaman.model.LevelTestSupport.{maze, startingLevel}
import it.unibo.pps.scalaman.model.collectibles.Collectibles
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class GameModeSpec extends AnyFunSuite:

  test("normal mode preserves the standard victory and defeat rules") {
    val won = startingLevel.copy(collectibles = Collectibles(Set.empty))
    val lost = startingLevel.copy(progress = LevelProgress(0))

    assert(won.status == Victory)
    assert(lost.status == Defeat)
  }

  test("a timed mode remains running before its limit") {
    val timed = LevelState.from(maze, GameMode.Timed(1.second)).ticking(999.millis)

    assert(timed.status == Running)
  }

  test("a timed mode loses when its limit expires") {
    val timed = LevelState.from(maze, GameMode.Timed(1.second)).ticking(1.second)

    assert(timed.status == Defeat)
  }

  test("a timed mode loses at the limit even when the last collectible is gone") {
    val timed = LevelState
      .from(maze, GameMode.Timed(1.second))
      .copy(collectibles = Collectibles(Set.empty), clock = GameClock(1.second))

    assert(timed.status == Defeat)
  }

  test("a timed mode requires a positive time limit") {
    assertThrows[IllegalArgumentException] {
      GameMode.Timed(0.seconds)
    }
  }

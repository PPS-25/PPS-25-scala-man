package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.LevelTestSupport.{levelWith, timePerPos}
import org.scalatest.funsuite.AnyFunSuite

class LevelMovementTest extends AnyFunSuite:

  private def ticks(level: LevelState, n: Int): LevelState =
    (1 to n).foldLeft(level)((l, _) => LevelState.pipeline(timePerPos).tick(l))

  /** A level with no enemies. */
  private def alone(at: Position): LevelState = levelWith(at).copy(enemies = Vector.empty)

  test("the player keeps going the way it faces") {
    assert(ticks(levelWith(Position(1, 1)), 4).player.currentPos == Position(1, 4))
  }

  test("the player stops wen the way it faces is blocked") {
    val stopped = ticks(alone(Position(1, 5)), 1)
    assert(stopped.player.currentPos == Position(1, 5))
    assert(!stopped.player.isMoving)
  }

  test("a turn asked for mid-crossing is taken on arrival") {
    val crossing = ticks(alone(Position(1, 4)), 1)
    val arrived = ticks(crossing.playerAsking(Direction.Down), 1)
    assert(arrived.player.movement.exists(_.to == Position(2, 5)))
  }

  test("a turn a wall refuses is kept until an opening allows it") {
    val after = ticks(alone(Position(1, 2)).playerAsking(Direction.Down), 5)
    assert(after.player.currentPos == Position(2, 5))
    assert(after.requestedDirection.isEmpty)
  }

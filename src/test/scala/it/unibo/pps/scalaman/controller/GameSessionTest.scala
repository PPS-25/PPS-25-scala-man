package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.{Direction, LevelState, Position}
import it.unibo.pps.scalaman.model.LevelTestSupport.{levelWith, startingLevel, timePerPos}
import it.unibo.pps.scalaman.model.collectibles.Collectibles
import org.scalatest.funsuite.AnyFunSuite

class GameSessionTest extends AnyFunSuite:

  private val drawNothing: LevelView => Unit = _ => ()
  private val spawn = Position(1, 1)
  private def sessionOn(level: LevelState): GameSession =
    GameSession.starting(level, drawNothing)

  test("the first frame only records when it happened, without changing the level") {
    val session = sessionOn(levelWith(spawn))
    val atFirstFrame = session.advancedToFrame(1000L)
    assert(atFirstFrame.lastFrame.contains(1000L))
    assert(atFirstFrame.level == session.level)
  }

  test("a paused session does not advance") {
    val runningSession = sessionOn(levelWith(spawn)).advancedToFrame(0L)
    val pausedSession = runningSession.togglePause.advancedToFrame(timePerPos.toNanos)
    assert(pausedSession.level.player == runningSession.level.player)
  }

  test("a resumed session advances again") {
    val paused = sessionOn(levelWith(spawn)).advancedToFrame(0L).togglePause
    val resumed = paused.togglePause.advancedToFrame(timePerPos.toNanos)
    assert(resumed.level.player.isMoving)
  }

  test("a requested direction reaches the level") {
    val askedDirection = sessionOn(levelWith(spawn))
      .advancedToFrame(1000L)
      .requestingDirection(Direction.Down)
      .advancedToFrame(timePerPos.toNanos)
    assert(askedDirection.level.player.movement.exists(_.to == Position(2, 1)))
    assert(askedDirection.requested.isEmpty)
  }

  test("a session is over when its level is over") {
    assert(sessionOn(startingLevel.copy(collectibles = Collectibles(Set.empty))).isOver)
  }

package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.{Direction, LevelState, Position}
import it.unibo.pps.scalaman.model.LevelTestSupport.{levelWith, startingLevel, timePerPos}
import it.unibo.pps.scalaman.model.collectibles.Collectibles
import org.scalatest.funsuite.AnyFunSuite
import it.unibo.pps.scalaman.presentation.LevelView

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

class GameSessionTest extends AnyFunSuite:

  private val drawNothing: LevelView => Unit = _ => ()
  private val spawn = Position(1, 1)
  private def sessionOn(level: LevelState): GameSession =
    GameSession.starting(level, drawNothing)

  private def readyOn(level: LevelState): GameSession =
    sessionOn(level).advancedToFrame(0L).advancedToFrame(GameSession.LeadIn.toNanos)

  private def frameAfterStart(after: FiniteDuration): Long =
    GameSession.LeadIn.toNanos + after.toNanos

  test("the first frame only records when it happened, without changing the level") {
    val session = sessionOn(levelWith(spawn))
    val atFirstFrame = session.advancedToFrame(1000L)
    assert(atFirstFrame.lastFrame.contains(1000L))
    assert(atFirstFrame.level == session.level)
  }

  test("a paused session does not advance") {
    val runningSession = readyOn(levelWith(spawn))
    val pausedSession = runningSession.togglePause.advancedToFrame(frameAfterStart(timePerPos))
    assert(pausedSession.level.player == runningSession.level.player)
  }

  test("a resumed session advances again") {
    val paused = readyOn(levelWith(spawn)).togglePause
    val resumed = paused.togglePause.advancedToFrame(frameAfterStart(timePerPos))
    assert(resumed.level.player.isMoving)
  }

  test("a requested direction reaches the level") {
    val askedDirection = readyOn(levelWith(spawn))
      .requestingDirection(Direction.Down)
      .advancedToFrame(frameAfterStart(timePerPos))
    assert(askedDirection.level.player.movement.exists(_.to == Position(2, 1)))
    assert(askedDirection.level.requestedDirection.isEmpty)
  }

  test("a frame that arrived before the one before it does not take the clock back") {
    val running = readyOn(levelWith(spawn)).advancedToFrame(frameAfterStart(10.seconds))
    assert(running.advancedToFrame(0L).level.clock == running.level.clock)
  }

  test("a frame the machine took too long over advances a game by no more than a step") {
    val jumped = readyOn(levelWith(spawn)).advancedToFrame(frameAfterStart(10.seconds))
    assert(jumped.level.clock.elapsed == GameSession.LongestStep)
  }

  test("nobody moves and no time passes while a game is counting down") {
    val counting =
      sessionOn(levelWith(spawn)).advancedToFrame(0L).advancedToFrame(2.seconds.toNanos)

    assert(!counting.level.player.isMoving)
    assert(counting.level.enemies.forall(!_.entity.isMoving))
    assert(counting.level.clock.elapsed == Duration.Zero)
  }

  test("a game counts three, two, one and then go") {
    val counted = List(900, 1900, 2900, 3400, 3600)
      .map(waited =>
        sessionOn(levelWith(spawn)).advancedToFrame(0L).advancedToFrame(waited.millis.toNanos)
      )
      .map(_.countdown)

    assert(counted == List(Some(3), Some(2), Some(1), Some(0), None))
  }

  test("a game held on hold does not count down") {
    val onHold = sessionOn(levelWith(spawn))
      .advancedToFrame(0L)
      .togglePause
      .advancedToFrame(2.seconds.toNanos)

    assert(onHold.countdown.contains(3))
  }

  test("a turn asked for while counting down is taken as soon as the game starts") {
    val asked = sessionOn(levelWith(spawn))
      .advancedToFrame(0L)
      .requestingDirection(Direction.Down)
      .advancedToFrame(GameSession.LeadIn.toNanos)
      .advancedToFrame(frameAfterStart(timePerPos))

    assert(asked.level.player.movement.exists(_.to == Position(2, 1)))
  }

  test("a session is over when its level is over") {
    assert(sessionOn(startingLevel.copy(collectibles = Collectibles(Set.empty))).isOver)
  }

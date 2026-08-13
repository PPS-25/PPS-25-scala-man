package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.LoopState.{Paused, Running, Stopped}
import org.scalatest.funsuite.AnyFunSuite

class GameLoopTest extends AnyFunSuite:
  test("GameLoop is created in the not started state") {
    assert(GameLoop().state == LoopState.NotStarted)
  }

  test("GameLoop can be paused") {
    assert(GameLoop().start().pause().state == Paused)
  }

  test("GameLoop can be started") {
    assert(GameLoop().start().state == Running)
  }

  test("GameLoop can be stopped") {
    assert(GameLoop().start().stop().state == Stopped)
  }

  test("Starting an already started loop throws") {
    assertThrows[IllegalArgumentException] {
      GameLoop().start().start()
    }
  }

  test("Pausing a loop that hasn't started throws") {
    assertThrows[IllegalArgumentException] {
      GameLoop().pause()
    }
  }

  test("Pausing an already paused loop throws") {
    assertThrows[IllegalArgumentException] {
      GameLoop().start().pause().pause()
    }
  }

  test("Resuming a loop that hasn't started throws") {
    assertThrows[IllegalArgumentException] {
      GameLoop().resume()
    }
  }

  test("resuming a running loop throws") {
    assertThrows[IllegalArgumentException] {
      GameLoop().start().resume()
    }
  }

  test("Stopping an already stopped loop throws") {
    assertThrows[IllegalArgumentException] {
      GameLoop().start().stop().stop()
    }
  }

  test("No operation can be performed on a stopped loop") {
    val gameLoop = GameLoop().start().stop()
    assertThrows[IllegalArgumentException] {
      gameLoop.start()
    }
    assertThrows[IllegalArgumentException] {
      gameLoop.pause()
    }
    assertThrows[IllegalArgumentException] {
      gameLoop.resume()
    }
  }

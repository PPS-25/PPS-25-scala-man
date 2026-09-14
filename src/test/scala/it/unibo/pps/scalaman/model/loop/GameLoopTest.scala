package it.unibo.pps.scalaman.model.loop

import it.unibo.pps.scalaman.model.*
import it.unibo.pps.scalaman.model.LoopState.{NotStarted, Paused, Running}
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

  test("toggling a running loop pauses it") {
    assert(GameLoop().start().toggled.state == LoopState.Paused)
  }

  test("toggling a paused loop resumes it") {
    assert(GameLoop().start().pause().toggled.state == LoopState.Running)
  }

  test("toggling a loop that never started does nothing") {
    assert(GameLoop().toggled.state == NotStarted)
  }

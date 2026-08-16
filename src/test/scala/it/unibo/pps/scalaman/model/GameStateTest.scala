package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.GameState.{Defeat, Paused, Running, Victory}
import org.scalatest.funsuite.AnyFunSuite

class GameStateTest extends AnyFunSuite:

  test("The initial game state is running") {
    assert(GameState.initial == Running)
  }

  test("A running game can be paused") {
    assert(Running.pause() == Paused)
  }

  test("A paused game can be resumed") {
    assert(Paused.resume() == Running)
  }

  test("A running game can be won or lost") {
    assert(Running.win() == Victory)
    assert(Running.lose() == Defeat)
  }

  test("A paused game can still reach a terminal state") {
    assert(Paused.win() == Victory)
    assert(Paused.lose() == Defeat)
  }

  test("Victory and defeat are terminal states") {
    assert(Victory.isTerminal)
    assert(Defeat.isTerminal)
    assert(!Running.isTerminal)
    assert(!Paused.isTerminal)
  }

  test("Only running games can be updated") {
    assert(Running.canUpdate)
    assert(!Paused.canUpdate)
    assert(!Victory.canUpdate)
    assert(!Defeat.canUpdate)
  }

  test("Terminal states reject further transitions") {
    assertThrows[IllegalArgumentException] {
      Victory.pause()
    }
    assertThrows[IllegalArgumentException] {
      Victory.resume()
    }
    assertThrows[IllegalArgumentException] {
      Victory.win()
    }
    assertThrows[IllegalArgumentException] {
      Victory.lose()
    }
    assertThrows[IllegalArgumentException] {
      Defeat.pause()
    }
    assertThrows[IllegalArgumentException] {
      Defeat.resume()
    }
    assertThrows[IllegalArgumentException] {
      Defeat.win()
    }
    assertThrows[IllegalArgumentException] {
      Defeat.lose()
    }
  }

  test("Invalid transitions from running and paused states are rejected") {
    assertThrows[IllegalArgumentException] {
      Paused.pause()
    }
    assertThrows[IllegalArgumentException] {
      Running.resume()
    }
  }

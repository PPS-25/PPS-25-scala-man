package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.GameState.{Defeat, Running, Victory}
import it.unibo.pps.scalaman.model.LoopState
import org.scalatest.funsuite.AnyFunSuite

class ScreenTest extends AnyFunSuite:

  test("a game that is counting down is not being played yet") {
    assert(Screen.of(LoopState.Running, Running, Some(3)) == Screen.Starting(3))
  }

  test("a game about to go is still counting down") {
    assert(Screen.of(LoopState.Running, Running, Some(0)) == Screen.Starting(0))
  }

  test("a game on hold reads as on hold, even while it counts down") {
    assert(Screen.of(LoopState.Paused, Running, Some(3)) == Screen.Paused)
  }

  test("the menu is shown until a game is started") {
    assert(Screen.of(LoopState.NotStarted, Running) == Screen.Menu)
  }

  test("a game being played is shown") {
    assert(Screen.of(LoopState.Running, Running) == Screen.Playing)
  }

  test("a game on hold is shown as paused") {
    assert(Screen.of(LoopState.Paused, Running) == Screen.Paused)
  }

  test("a game that was won is shown as over, before the loop is even stopped") {
    assert(Screen.of(LoopState.Running, Victory) == Screen.Over(Outcome.Victory))
  }

  test("a game that was lost is shown as over") {
    assert(Screen.of(LoopState.Running, Defeat) == Screen.Over(Outcome.Defeat))
  }

  test("an outcome is still shown once the loop has stopped") {
    assert(Screen.of(LoopState.Stopped, Victory) == Screen.Over(Outcome.Victory))
  }

  test("a loop stopped with nothing won or lost leaves the game") {
    assert(Screen.of(LoopState.Stopped, Running) == Screen.Menu)
  }

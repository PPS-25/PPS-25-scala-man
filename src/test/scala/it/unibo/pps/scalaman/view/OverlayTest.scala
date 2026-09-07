package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.app.Command
import it.unibo.pps.scalaman.model.GameState
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class OverlayTest extends AnyFunSuite:

  private val status =
    StatusBar(lives = 2, remaining = 4, applied = Set.empty, GameState.Running, 1200, 95.seconds)

  private def over(screen: Screen): Option[Overlay] = Overlay.of(screen, status)

  test("a game being played is covered by nothing") {
    assert(over(Screen.Playing).isEmpty)
  }

  test("the menu is a screen of its own, not a veil over a board") {
    assert(over(Screen.Menu).isEmpty)
  }

  test("a game on hold offers to restart, to resume, and to save and quit") {
    assert(
      over(Screen.Paused).map(_.choices) ==
        Some(Seq(Command.Restart, Command.Resume, Command.SaveAndQuit))
    )
  }

  test("a game that was won says so") {
    assert(over(Screen.Over(Outcome.Victory)).map(_.title) == Some("Victory"))
  }

  test("a game that was lost says so") {
    assert(over(Screen.Over(Outcome.Defeat)).map(_.title) == Some("Defeat"))
  }

  test("a game that is over tells what was reached") {
    assert(over(Screen.Over(Outcome.Victory)).map(_.lines) == Some(Seq("Score 1200", "Time 01:35")))
  }

  test("a game that is over offers to play again or to leave") {
    assert(
      over(Screen.Over(Outcome.Defeat)).map(_.choices) ==
        Some(Seq(Command.Restart, Command.BackToMenu))
    )
  }

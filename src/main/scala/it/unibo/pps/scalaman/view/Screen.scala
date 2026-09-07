package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.app.Command
import it.unibo.pps.scalaman.model.{GameState, LoopState}

/** How a game ended. */
enum Outcome:
  case Victory, Defeat

/** What the application is showing. */
enum Screen:
  case Menu, Playing, Paused
  case Over(outcome: Outcome)

object Screen:

  /** What to show. The loop knows whether a game is being played and whether it is on hold, the
    * game knows whether it has been won or lost: neither of the two answers on its own.
    */
  def of(loop: LoopState, state: GameState): Screen = (loop, state) match
    case (LoopState.NotStarted, _) => Menu
    case (_, GameState.Victory)    => Over(Outcome.Victory)
    case (_, GameState.Defeat)     => Over(Outcome.Defeat)
    case (LoopState.Paused, _)     => Paused
    case (LoopState.Stopped, _)    => Menu
    case (LoopState.Running, _)    => Playing

/** What is read over the board while the game is not being played. */
final case class Overlay(title: String, lines: Seq[String], choices: Seq[Command])

object Overlay:

  /** What covers the board, if anything. A game being played is covered by nothing, and the menu is
    * a screen of its own rather than a veil over a board.
    */
  def of(screen: Screen, status: StatusBar): Option[Overlay] = screen match
    case Screen.Paused =>
      Some(Overlay("Paused", Seq.empty, Seq(Command.Restart, Command.Resume, Command.SaveAndQuit)))
    case Screen.Over(outcome) =>
      Some(Overlay(titleOf(outcome), reached(status), Seq(Command.Restart, Command.BackToMenu)))
    case Screen.Playing | Screen.Menu => None

  private def titleOf(outcome: Outcome): String = outcome match
    case Outcome.Victory => "Victory"
    case Outcome.Defeat  => "Defeat"

  private def reached(status: StatusBar): Seq[String] =
    Seq(s"Score ${status.score}", s"Time ${status.timeDescribed}")

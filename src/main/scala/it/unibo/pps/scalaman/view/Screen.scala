package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.app.Command
import it.unibo.pps.scalaman.model.{GameState, LoopState}

enum Outcome:
  case Victory, Defeat

enum Screen:
  case Menu, Playing, Paused
  case Over(outcome: Outcome)

  case Starting(secondsLeft: Int)

object Screen:

  def of(loop: LoopState, state: GameState, startingIn: Option[Int] = None): Screen =
    (loop, state) match
      case (LoopState.NotStarted, _) => Menu
      case (_, GameState.Victory)    => Over(Outcome.Victory)
      case (_, GameState.Defeat)     => Over(Outcome.Defeat)
      case (LoopState.Paused, _)     => Paused
      case (LoopState.Stopped, _)    => Menu
      case (LoopState.Running, _)    => startingIn.fold(Playing)(Starting.apply)

final case class Overlay(title: String, lines: Seq[String], choices: Seq[Command])

object Overlay:

  def of(screen: Screen, status: => StatusBar): Option[Overlay] = screen match
    case Screen.Paused =>
      Some(Overlay("Paused", Seq.empty, Seq(Command.Restart, Command.Resume, Command.SaveAndQuit)))
    case Screen.Over(outcome) =>
      Some(Overlay(titleOf(outcome), reached(status), Seq(Command.Restart, Command.BackToMenu)))
    case Screen.Starting(secondsLeft) =>
      Some(Overlay(counted(secondsLeft), Seq.empty, Seq.empty))
    case Screen.Playing | Screen.Menu => None

  private def counted(secondsLeft: Int): String =
    if secondsLeft == 0 then "Go!" else secondsLeft.toString

  private def titleOf(outcome: Outcome): String = outcome match
    case Outcome.Victory => "Victory"
    case Outcome.Defeat  => "Defeat"

  private def reached(status: StatusBar): Seq[String] =
    Seq(s"Score ${status.score}", s"Time ${status.timePlayed}")

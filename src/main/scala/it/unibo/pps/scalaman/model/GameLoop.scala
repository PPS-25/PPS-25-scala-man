package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.LoopState.{NotStarted, Running, Paused, Stopped}

enum LoopState:
  case NotStarted
  case Running
  case Paused
  case Stopped

final case class GameLoop(state: LoopState = NotStarted):

  private def transition(validFrom: LoopState*)(to: LoopState): GameLoop =
    require(validFrom.contains(state), s"Invalid transition from $state to $to")
    copy(state = to)

  def toggled: GameLoop = state match
    case Running => pause()
    case Paused  => resume()
    case _       => this

  def start(): GameLoop =
    transition(NotStarted)(Running)

  def pause(): GameLoop =
    transition(Running)(Paused)

  def stop(): GameLoop =
    transition(Running, Paused)(Stopped)

  def resume(): GameLoop =
    transition(Paused)(Running)

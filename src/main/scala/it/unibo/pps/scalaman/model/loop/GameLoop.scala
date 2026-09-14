package it.unibo.pps.scalaman.model.loop

import it.unibo.pps.scalaman.model.LoopState.{NotStarted, Running, Paused}

enum LoopState:
  case NotStarted
  case Running
  case Paused

/** A game loop.
  * @param state
  *   the state of the game loop.
  */
final case class GameLoop(state: LoopState = NotStarted):

  /** Transitions the game loop to the state `to` if the loop's current state is one of `validFrom`
    * @param validFrom
    *   valid states for the transition
    * @param to
    *   resulting state
    * @return
    *   the new game loop
    */
  private def transition(validFrom: LoopState*)(to: LoopState): GameLoop =
    require(validFrom.contains(state), s"Invalid transition from $state to $to")
    copy(state = to)

  /** The loop after the player asks to pause or resume the game. */
  def toggled: GameLoop = state match
    case Running => pause()
    case Paused  => resume()
    case _       => this

  /** Starts the game loop.
    * @throws IllegalArgumentException
    *   if the loop had already started.
    * @return
    *   a running game loop
    */
  def start(): GameLoop =
    transition(NotStarted)(Running)

  /** Pauses the game loop.
    * @throws IllegalArgumentException
    *   if the loop was not running.
    * @return
    *   a paused game loop
    */
  def pause(): GameLoop =
    transition(Running)(Paused)

  /** Resumes a paused game loop.
    * @throws IllegalArgumentException
    *   if the loop was not paused.
    * @return
    *   running game loop.
    */
  def resume(): GameLoop =
    transition(Paused)(Running)

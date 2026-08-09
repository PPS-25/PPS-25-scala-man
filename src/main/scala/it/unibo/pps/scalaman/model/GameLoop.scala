package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.LoopState.{
  NotStarted,
  Running,
  Paused,
  Stopped
}

enum LoopState:
  case NotStarted
  case Running
  case Paused
  case Stopped

/** A game loop.
  * @param state
  *   the state of the game loop.
  */
final case class GameLoop(state: LoopState = NotStarted):

  /** Transitions the game loop to the state `to` if the loop's current state is
    * one of `validFrom`
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

  /** Stops the game loop. A game loop that was stopped can never resume again.
    * @throws IllegalArgumentException
    *   if the game loop was neither running nor paused.
    * @return
    *   a stopped game loop.
    */
  def stop(): GameLoop =
    transition(Running, Paused)(Stopped)

  /** Resumes a paused game loop.
    * @throws IllegalArgumentException
    *   if the loop was not paused.
    * @return
    *   running game loop.
    */
  def resume(): GameLoop =
    transition(Paused)(Running)

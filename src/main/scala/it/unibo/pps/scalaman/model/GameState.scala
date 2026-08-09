package it.unibo.pps.scalaman.model

/** Explicit state of the match.
  *
  * The game can run, be paused, or reach a terminal victory/defeat state. Once
  * a terminal state is reached, no further gameplay transitions are allowed.
  */
enum GameState:
  case Running, Paused, Victory, Defeat

  private def invalidTransition(to: GameState): IllegalArgumentException =
    IllegalArgumentException(s"Invalid transition from $this to $to")

  /** Returns whether the game is in a terminal state.
    *
    * Terminal states cannot transition to any other state and should stop
    * gameplay updates.
    */
  def isTerminal: Boolean = this match
    case Victory | Defeat => true
    case _                => false

  /** Returns whether gameplay updates are allowed in this state. */
  def canUpdate: Boolean = this == Running

  /** Pauses a running game.
    *
    * @throws IllegalArgumentException
    *   if the current state is not running.
    */
  def pause(): GameState = this match
    case Running => Paused
    case _       => throw invalidTransition(Paused)

  /** Resumes a paused game.
    *
    * @throws IllegalArgumentException
    *   if the current state is not paused.
    */
  def resume(): GameState = this match
    case Paused => Running
    case _      => throw invalidTransition(Running)

  /** Marks the game as a victory.
    *
    * @throws IllegalArgumentException
    *   if the current state is terminal.
    */
  def win(): GameState = this match
    case Running | Paused => Victory
    case _                => throw invalidTransition(Victory)

  /** Marks the game as a defeat.
    *
    * @throws IllegalArgumentException
    *   if the current state is terminal.
    */
  def lose(): GameState = this match
    case Running | Paused => Defeat
    case _                => throw invalidTransition(Defeat)

object GameState:
  val initial: GameState = GameState.Running

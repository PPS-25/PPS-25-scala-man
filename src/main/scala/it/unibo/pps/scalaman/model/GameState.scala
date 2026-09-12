package it.unibo.pps.scalaman.model

/** Outcome state of a level. Pause and resume are execution concerns owned by [[GameLoop]]. */
enum GameState:
  case Running, Victory, Defeat

  /** Returns whether the game is in a terminal state.
    *
    * Terminal states cannot transition to any other state and should stop gameplay updates.
    */
  def isTerminal: Boolean = this match
    case Victory | Defeat => true
    case _                => false

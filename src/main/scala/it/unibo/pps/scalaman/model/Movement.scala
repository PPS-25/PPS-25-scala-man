package it.unibo.pps.scalaman.model

import scala.concurrent.duration.{FiniteDuration, Duration}

/** A movement detailing the way a moving entity should move from a cell to
  * another, considering the remaining time to complete the transition.
  * @param from
  *   the starting cell.
  * @param to
  *   the ending cell.
  * @param remaining
  *   the remaining time to complete the movement.
  */
case class Movement(from: Cell, to: Cell, remaining: FiniteDuration):

  /** Advances the movement by a given amount of time.
    * @param elapsed
    *   the amount of time.
    */
  def advance(elapsed: FiniteDuration): Movement =
    copy(remaining = remaining - elapsed)

  /** Determines whether the movement has been completed.
    */
  def isComplete: Boolean =
    remaining <= Duration.Zero

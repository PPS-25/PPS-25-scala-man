package it.unibo.pps.scalaman.model

import scala.concurrent.duration.{FiniteDuration, Duration}

/** A movement detailing the way a moving entity should move from a position to another, considering
  * the remaining time to complete the transition.
  * @param from
  *   the starting position.
  * @param to
  *   the ending position.
  * @param remaining
  *   the remaining time to complete the movement.
  */
case class Movement(from: Position, to: Position, remaining: FiniteDuration):

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

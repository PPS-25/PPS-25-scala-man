package it.unibo.pps.scalaman.model.space

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
  require(remaining >= Duration.Zero, "movement time left cannot be negative")

  /** Advances the movement by a given amount of time.
    * @param elapsed
    *   the amount of time.
    */
  def advance(elapsed: FiniteDuration): Movement =
    require(elapsed >= Duration.Zero, "elapsed time cannot be negative")
    copy(remaining = (remaining - elapsed).max(Duration.Zero))

  /** Determines whether the movement has been completed.
    */
  def isComplete: Boolean =
    remaining <= Duration.Zero

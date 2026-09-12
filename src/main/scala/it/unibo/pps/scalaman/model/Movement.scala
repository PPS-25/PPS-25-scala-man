package it.unibo.pps.scalaman.model

import scala.concurrent.duration.{FiniteDuration, Duration}

case class Movement(from: Position, to: Position, remaining: FiniteDuration):
  require(remaining >= Duration.Zero, "movement time left cannot be negative")

  def advance(elapsed: FiniteDuration): Movement =
    require(elapsed >= Duration.Zero, "elapsed time cannot be negative")
    copy(remaining = (remaining - elapsed).max(Duration.Zero))

  def isComplete: Boolean =
    remaining <= Duration.Zero

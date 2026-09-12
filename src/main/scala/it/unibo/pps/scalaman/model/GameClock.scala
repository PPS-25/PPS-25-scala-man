package it.unibo.pps.scalaman.model

import scala.concurrent.duration.{Duration, FiniteDuration}
final case class GameClock(elapsed: FiniteDuration = Duration.Zero):
  def advance(delta: FiniteDuration): GameClock =
    require(delta >= Duration.Zero, "delta must not be negative")
    copy(elapsed = elapsed + delta)

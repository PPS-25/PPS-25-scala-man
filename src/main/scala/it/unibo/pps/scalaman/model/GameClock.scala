package it.unibo.pps.scalaman.model

import scala.concurrent.duration.{Duration, FiniteDuration}

/** Immutable clock that tracks elapsed time. Does not depend on the system
  * clock.
  * @param elapsed
  *   the elapsed time
  */
final case class GameClock(elapsed: FiniteDuration = Duration.Zero):

  /** Returns a new clock with delta added to the elapsed time.
    * @throws IllegalArgumentException
    *   if delta is negative
    * @param delta
    *   the elapsed time to advance
    * @return
    *   a new GameClock
    */
  def advance(delta: FiniteDuration): GameClock =
    require(delta >= Duration.Zero, "delta must not be negative")
    copy(elapsed = elapsed + delta)

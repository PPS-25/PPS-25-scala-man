package it.unibo.pps.scalaman.model.effects

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/** How long an effect lasts from the moment it is granted. */
trait BonusDuration:
  def of(effect: BonusEffect): FiniteDuration

object BonusDuration:
  private val SlowDownLasts: FiniteDuration = 5.seconds
  private val InvulnerabilityLasts: FiniteDuration = 8.seconds

  /** The durations the game is played with. */
  given standardDurations: BonusDuration with
    def of(effect: BonusEffect): FiniteDuration = effect match
      case BonusEffect.SlowDown        => SlowDownLasts
      case BonusEffect.Invulnerability => InvulnerabilityLasts

/** How much of the passing time the enemies can experience. */
trait Slowdown:
  def slowed(delta: FiniteDuration): FiniteDuration

object Slowdown:

  /** Half the speed: half the time experienced. */
  given halvedSpeed: Slowdown with
    def slowed(delta: FiniteDuration): FiniteDuration = delta / 2

package it.unibo.pps.scalaman.model.effects

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/** How long the effect granted by a bonus lasts. */
trait BonusDuration:
  /** How long an effect lasts from the moment it is granted. */
  def of(effect: BonusEffect): FiniteDuration

object BonusDuration:
  private val SlowDownLasts: FiniteDuration = 5.seconds
  private val InvulnerabilityLasts: FiniteDuration = 8.seconds

  /** The durations the game is played with. */
  given standardDurations: BonusDuration with
    def of(effect: BonusEffect): FiniteDuration = effect match
      case BonusEffect.SlowDown        => SlowDownLasts
      case BonusEffect.Invulnerability => InvulnerabilityLasts

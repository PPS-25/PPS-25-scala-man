package it.unibo.pps.scalaman.model.effects

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait BonusDuration:
  def of(effect: BonusEffect): FiniteDuration

object BonusDuration:
  private val SlowDownLasts: FiniteDuration = 5.seconds
  private val InvulnerabilityLasts: FiniteDuration = 8.seconds

  given standardDurations: BonusDuration with
    def of(effect: BonusEffect): FiniteDuration = effect match
      case BonusEffect.SlowDown        => SlowDownLasts
      case BonusEffect.Invulnerability => InvulnerabilityLasts

trait Slowdown:
  def slowed(delta: FiniteDuration): FiniteDuration

object Slowdown:

  given halvedSpeed: Slowdown with
    def slowed(delta: FiniteDuration): FiniteDuration = delta / 2

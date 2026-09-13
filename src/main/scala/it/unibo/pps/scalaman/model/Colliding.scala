package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusEffect}

import scala.concurrent.duration.FiniteDuration

extension (progress: LevelProgress)
  /** The progress after meeting an enemy, which costs a life unless invulnerability is applied.
    * Whether the meeting counts as a collision is decided elsewhere: here it already did.
    */
  def afterCollision(
      effects: ActiveEffects,
      now: FiniteDuration
  ): LevelProgress =
    if effects.isActive(BonusEffect.Invulnerability, now) then progress
    else progress.lose

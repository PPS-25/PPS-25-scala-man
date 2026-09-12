package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusEffect}

import scala.concurrent.duration.FiniteDuration

extension (progress: LevelProgress)
  def afterCollision(
      effects: ActiveEffects,
      now: FiniteDuration
  ): LevelProgress =
    if effects.isActive(BonusEffect.Invulnerability, now) then progress
    else progress.lose

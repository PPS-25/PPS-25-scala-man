package it.unibo.pps.scalaman.model.effects

import scala.concurrent.duration.{Duration, FiniteDuration}

trait ActiveEffects:
  def active(now: FiniteDuration): Set[BonusEffect]
  def isActive(effect: BonusEffect, now: FiniteDuration): Boolean =
    active(now).contains(effect)

  def enemyDelta(delta: FiniteDuration, now: FiniteDuration)(using
      slowdown: Slowdown
  ): FiniteDuration =
    if isActive(BonusEffect.SlowDown, now) then slowdown.slowed(delta)
    else delta

  def activate(
      effect: BonusEffect,
      now: FiniteDuration,
      duration: FiniteDuration
  ): ActiveEffects

  def updated(now: FiniteDuration): ActiveEffects

  def remaining(now: FiniteDuration): Map[BonusEffect, FiniteDuration]

object ActiveEffects:

  def empty: ActiveEffects = UntilExpiration(Map.empty)

  def fromRemaining(
      now: FiniteDuration,
      remaining: Map[BonusEffect, FiniteDuration]
  ): ActiveEffects =
    require(remaining.values.forall(_ > Duration.Zero), "an active bonus must have time left")
    UntilExpiration(remaining.view.mapValues(now + _).toMap)

  private final case class UntilExpiration(
      expirations: Map[BonusEffect, FiniteDuration]
  ) extends ActiveEffects:

    def active(now: FiniteDuration): Set[BonusEffect] = stillRunning(now).keySet

    def updated(now: FiniteDuration): ActiveEffects =
      UntilExpiration(stillRunning(now))

    def remaining(now: FiniteDuration): Map[BonusEffect, FiniteDuration] =
      stillRunning(now).view.mapValues(_ - now).toMap

    private def stillRunning(now: FiniteDuration) =
      expirations.filter((_, until) => now < until)

    def activate(
        effect: BonusEffect,
        now: FiniteDuration,
        duration: FiniteDuration
    ): ActiveEffects =
      require(duration > Duration.Zero, "a bonus must last some time")
      UntilExpiration(expirations + (effect -> (now + duration)))

package it.unibo.pps.scalaman.model.effects

import scala.concurrent.duration.{Duration, FiniteDuration}

/** The temporary effects currently applied to the game. Time is taken as a parameter rather than
  * read from a clock, so that the whole abstraction stays pure.
  */
trait ActiveEffects:

  /** The effects still applied at a given instant. */
  def active(now: FiniteDuration): Set[BonusEffect]

  /** Whether an effect is still applied at a given instant. */
  def isActive(effect: BonusEffect, now: FiniteDuration): Boolean =
    active(now).contains(effect)

  /** How long the enemies wait between steps, given how long they usually wait. Asked for the
    * enemies only: the player is never held back.
    */
  def enemyStepInterval(betweenSteps: FiniteDuration, now: FiniteDuration)(using
      slowdown: Slowdown
  ): FiniteDuration =
    if isActive(BonusEffect.SlowDown, now) then slowdown.stretch(betweenSteps)
    else betweenSteps

  /** Grants an effect, postponing its expiration by the full duration when it is already applied.
    * @throws IllegalArgumentException
    *   if the duration is not positive.
    */
  def activate(
      effect: BonusEffect,
      now: FiniteDuration,
      duration: FiniteDuration
  ): ActiveEffects

  /** The effects left once the ones that expired are dropped. */
  def updated(now: FiniteDuration): ActiveEffects

object ActiveEffects:

  /** No effect applied. */
  def empty: ActiveEffects = UntilExpiration(Map.empty)

  private final case class UntilExpiration(
      expirations: Map[BonusEffect, FiniteDuration]
  ) extends ActiveEffects:

    def active(now: FiniteDuration): Set[BonusEffect] = stillRunning(now).keySet

    def updated(now: FiniteDuration): ActiveEffects =
      UntilExpiration(stillRunning(now))

    private def stillRunning(now: FiniteDuration) =
      expirations.filter((_, until) => now < until)

    def activate(
        effect: BonusEffect,
        now: FiniteDuration,
        duration: FiniteDuration
    ): ActiveEffects =
      require(duration > Duration.Zero, "a bonus must last some time")
      UntilExpiration(expirations + (effect -> (now + duration)))

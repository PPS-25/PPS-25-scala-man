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

  /** Grants an effect, postponing its expiration by the full duration when the effect is already
    * active.
    * @param effect
    *   the effect to grant.
    * @param now
    *   the current elapsed time.
    * @param duration
    *   how long the effect lasts from now on.
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

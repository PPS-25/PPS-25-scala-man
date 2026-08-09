package it.unibo.pps.scalaman.model

import scala.concurrent.duration.FiniteDuration

extension (collectibles: Collectibles)
  /** Collects what is placed on the cell the player occupies. A player that is
    * still moving towards a cell does not occupy it yet, so it collects
    * nothing.
    * @param player
    *   the entity controlled by the player.
    */
  def collectedBy(player: MovingEntity): Collected =
    collectibles.collect(player.currentCell)

extension (effects: ActiveEffects)
  /** Grants the effect carried by a collected bonus, leaving the effects
    * untouched when nothing or a standard collectible was collected.
    * @param collected
    *   the element just collected, if there was one.
    * @param now
    *   the current elapsed time.
    */
  def grantedBy(collected: Option[Collectible], now: FiniteDuration)(using
      duration: BonusDuration
  ): ActiveEffects = collected match
    case Some(Collectible.Bonus(_, effect)) =>
      effects.activate(effect, now, duration.of(effect))
    case _ => effects

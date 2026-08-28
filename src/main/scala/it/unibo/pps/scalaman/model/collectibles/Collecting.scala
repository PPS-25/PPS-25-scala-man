package it.unibo.pps.scalaman.model.collectibles

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusDuration}
import it.unibo.pps.scalaman.model.entities.MovingEntity
import it.unibo.pps.scalaman.model.score.ScoreTracker
import it.unibo.pps.scalaman.model.score.ScoringEvent.{BasicItem, BonusItem}

import scala.concurrent.duration.FiniteDuration

extension (collectibles: Collectibles)
  /** Collects what is placed on the position the player occupies. A player that is still moving
    * towards a position does not occupy it yet, so it collects nothing.
    * @param player
    *   the entity controlled by the player.
    */
  def collectedBy(player: MovingEntity): Collected =
    collectibles.collect(player.currentPos)

extension (effects: ActiveEffects)
  /** Grants the effect carried by a collected bonus, leaving the effects untouched when nothing or
    * a standard collectible was collected.
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

extension (score: ScoreTracker)
  /** Awards the points carried by a collected element.
    * @param collected
    *   the element just collected.
    */
  def awardedFor(collected: Option[Collectible]): ScoreTracker = collected match
    case Some(Collectible.Basic(_))    => score.increaseScore(BasicItem)
    case Some(Collectible.Bonus(_, _)) => score.increaseScore(BonusItem)
    case None                          => score

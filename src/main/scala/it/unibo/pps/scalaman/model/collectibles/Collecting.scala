package it.unibo.pps.scalaman.model.collectibles

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusDuration}
import it.unibo.pps.scalaman.model.entities.MovingEntity
import it.unibo.pps.scalaman.model.score.{ScoreTracker, ScoringRule}
import it.unibo.pps.scalaman.model.score.ScoringEvent.{BasicItem, BonusItem}

import scala.concurrent.duration.FiniteDuration

extension (collectibles: Collectibles)
  def collectedBy(player: MovingEntity): Collected =
    collectibles.collect(player.currentPos)

extension (effects: ActiveEffects)
  def grantedBy(collected: Option[Collectible], now: FiniteDuration)(using
      duration: BonusDuration
  ): ActiveEffects = collected match
    case Some(Collectible.Bonus(_, effect)) =>
      effects.activate(effect, now, duration.of(effect))
    case _ => effects

extension (score: ScoreTracker)
  def awardedFor(collected: Option[Collectible])(using ScoringRule): ScoreTracker = collected match
    case Some(Collectible.Basic(_))    => score.increaseScore(BasicItem)
    case Some(Collectible.Bonus(_, _)) => score.increaseScore(BonusItem)
    case None                          => score

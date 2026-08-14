package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.collectibles.{Collectibles, collectedBy, grantedBy}
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusDuration, Slowdown}

import scala.concurrent.duration.FiniteDuration

/** A level being played: what is left on the map, what the bonuses are doing, how the player is
  * doing, and how long the level has been running. The maze and the enemies will join once they
  * exist.
  */
final case class LevelState(
    player: MovingEntity,
    collectibles: Collectibles,
    effects: ActiveEffects,
    progress: LevelProgress,
    clock: GameClock = GameClock()
):

  /** The level after some time has passed. */
  def ticking(delta: FiniteDuration): LevelState = copy(clock = clock.advance(delta))

  /** The level after the player picked up what it stands on, effect included. */
  def collecting(using BonusDuration): LevelState =
    val picked = collectibles.collectedBy(player)
    copy(
      collectibles = picked.left,
      effects = effects.grantedBy(picked.element, clock.elapsed)
    )

  /** How long the enemies wait between steps, given how long they usually wait. */
  def enemyStepInterval(betweenSteps: FiniteDuration)(using Slowdown): FiniteDuration =
    effects.enemyStepInterval(betweenSteps, clock.elapsed)

  /** The level with the effects that expired dropped. */
  def withoutExpiredEffects: LevelState = copy(effects = effects.updated(clock.elapsed))

object LevelState:

  /** The stages a level goes through on each tick. The ones left out belong to other parts of the
    * game.
    */
  def pipeline(using BonusDuration): GameStateUpdatePipeline[LevelState] =
    GameStateUpdatePipeline(
      collectItems = _.collecting,
      applyBonuses = _.withoutExpiredEffects
    )

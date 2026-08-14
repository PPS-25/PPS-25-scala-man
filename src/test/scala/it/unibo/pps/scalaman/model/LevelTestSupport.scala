package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.effects.BonusEffect.Invulnerability
import it.unibo.pps.scalaman.model.collectibles.Collectible.{Basic, Bonus}
import it.unibo.pps.scalaman.model.collectibles.Collectibles
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusDuration}
import it.unibo.pps.scalaman.model.Direction.Right

import scala.concurrent.duration.DurationInt

/** A level holding one standard collectible and one bonus, shared by the suites about levels. */
object LevelTestSupport:
  val timePerPos = 100.millis
  val item: Basic = Basic(Position(0, 1))
  val bonus: Bonus = Bonus(Position(0, 2), Invulnerability)
  val lasting = summon[BonusDuration].of(Invulnerability)

  /** The level with the player standing on a given position. */
  def levelWith(playerAt: Position): LevelState = LevelState(
    player = MovingEntity(playerAt, Right, timePerPos),
    collectibles = Collectibles(Set(item, bonus)),
    effects = ActiveEffects.empty,
    progress = LevelProgress.initial
  )

  /** The level with the player standing on nothing. */
  val startingLevel: LevelState = levelWith(Position(0, 0))

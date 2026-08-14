package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.collectibles.Collectible
import it.unibo.pps.scalaman.model.map.Enemy
import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.{LevelState, Position}

/** What a level shows to whoever draws it, and nothing more.
  */
final case class LevelView(
    player: Position,
    enemies: Vector[Enemy],
    collectibles: Set[Collectible],
    remaining: Int,
    lives: Int,
    applied: Set[BonusEffect]
)

object LevelView:

  /** What the view is shown of a level. */
  def of(level: LevelState): LevelView = LevelView(
    player = level.player.currentPos,
    enemies = level.enemies,
    collectibles = level.collectibles.placed,
    remaining = level.collectibles.remaining,
    lives = level.progress.lives,
    applied = level.effects.active(level.clock.elapsed)
  )

  /** Notifies whoever draws a level, whenever a tick changes what it is shown. */
  def rendering: Rendering[LevelState, LevelView] = Rendering(of)

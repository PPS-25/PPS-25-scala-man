package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.collectibles.Collectible
import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.entities.{Enemy, MovingEntity}
import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.{Direction, GameState, LevelState, Position}

import scala.concurrent.duration.{DurationLong, FiniteDuration}

/** What a level shows to whoever draws it, and nothing more.
  */
final case class LevelView(
    player: RenderedMovement,
    enemies: Vector[RenderedEnemyMovement],
    collectibles: Set[Collectible],
    remaining: Int,
    lives: Int,
    applied: Set[BonusEffect],
    status: GameState,
    score: Int,
    elapsed: FiniteDuration
)

object LevelView:

  /** What the view is shown of a level. */
  def of(level: LevelState): LevelView = LevelView(
    player = RenderedMovement.of(level.player),
    enemies = level.enemies.map(enemy =>
      RenderedEnemyMovement(RenderedMovement.of(enemy.entity), enemy.kind)
    ),
    collectibles = level.collectibles.placed,
    remaining = level.collectibles.remaining,
    lives = level.progress.lives,
    applied = level.effects.active(level.clock.elapsed),
    status = level.status,
    score = level.score.currentScore,
    elapsed = level.clock.elapsed.toSeconds.seconds
  )

  /** Notifies whoever draws a level, whenever a tick changes what it is shown. */
  def rendering: Rendering[LevelState, LevelView] = Rendering(of)

/** Where a MovingEntity is drawn. Contains the cell the entity is leaving, the one it is reaching,
  * and how far along the movement it is. A still entity is leaving and reaching the same cell.
  */
final case class RenderedMovement(from: Position, to: Position, progress: Double, facing: Direction)

object RenderedMovement:
  def of(entity: MovingEntity): RenderedMovement = entity.movement match
    case Some(movement) =>
      RenderedMovement(
        movement.from,
        movement.to,
        1 - (movement.remaining / entity.timePerPos),
        entity.facing
      )
    case None => RenderedMovement(entity.currentPos, entity.currentPos, 0, entity.facing)

/** Where specifically an enemy is drawn. */
final case class RenderedEnemyMovement(at: RenderedMovement, kind: EnemyKind)

package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.collectibles.Collectible
import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.entities.MovingEntity
import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.{Direction, GameState, LeaderboardMode, LevelState, Position}

import scala.concurrent.duration.{DurationLong, FiniteDuration}

/** Immutable read model shared by the controller and graphical view. */
final case class LevelView(
    player: RenderedMovement,
    enemies: Vector[RenderedEnemyMovement],
    collectibles: Set[Collectible],
    remaining: Int,
    lives: Int,
    applied: Set[BonusEffect],
    status: GameState,
    score: Int,
    elapsed: FiniteDuration,
    timeLeft: Option[FiniteDuration],
    mode: LeaderboardMode = LeaderboardMode.Classic
)

object LevelView:
  def of(level: LevelState): LevelView = LevelView(
    RenderedMovement.of(level.player),
    level.enemies.map(enemy =>
      RenderedEnemyMovement(RenderedMovement.of(enemy.entity), enemy.kind)
    ),
    level.collectibles.placed,
    level.collectibles.remaining,
    level.progress.lives,
    level.effects.active(level.clock.elapsed),
    level.status,
    level.liveScore,
    level.clock.elapsed.toSeconds.seconds,
    level.mode.timeLeft(level.clock).map(wholeSecondsUp),
    LeaderboardMode.of(level.mode)
  )

  def rendering: Rendering[LevelState, LevelView] = Rendering(of)

private def wholeSecondsUp(left: FiniteDuration): FiniteDuration =
  ((left.toMillis + 999) / 1000).seconds

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

final case class RenderedEnemyMovement(at: RenderedMovement, kind: EnemyKind)

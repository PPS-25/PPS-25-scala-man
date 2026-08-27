package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.{LevelState, MovingEntity, Position}
import it.unibo.pps.scalaman.model.Direction.Right
import it.unibo.pps.scalaman.model.map.{Enemy, EnemyKind, RawMap, Tile, ValidatedMap}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class EnemyAiStageSpec extends AnyFunSuite:
  private val map = ValidatedMap(
    raw = RawMap(
      Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(
          Tile.Wall,
          Tile.Spawn,
          Tile.Collectible,
          Tile.Floor,
          Tile.Floor,
          Tile.Floor,
          Tile.Wall
        ),
        Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
        Vector(
          Tile.Wall,
          Tile.Hunter,
          Tile.Floor,
          Tile.Anticipator,
          Tile.Floor,
          Tile.Floor,
          Tile.Wall
        ),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
      )
    ),
    spawn = Position(1, 1),
    collectibles = Set(Position(1, 2)),
    enemies = Set(
      Enemy(Position(3, 1), EnemyKind.Hunter),
      Enemy(Position(3, 3), EnemyKind.Anticipator)
    ),
    teleports = Map.empty
  )

  private val level = LevelState
    .from(map)
    .copy(
      player = MovingEntity(Position(2, 2), Right, 200.millis),
      playerPreviousPos = Some(Position(2, 1))
    )

  test("moves enemies with their selected strategies once a step is due") {
    val moved = EnemyAiStage.stage(level.ticking(LevelState.BetweenEnemySteps))

    assert(
      moved.enemies.map(enemy => enemy.kind -> enemy.position).toMap == Map(
        EnemyKind.Hunter -> Position(2, 1),
        EnemyKind.Anticipator -> Position(3, 4)
      )
    )
  }

  test("does not move enemies before a step is due") {
    assert(EnemyAiStage.stage(level).enemies == level.enemies)
  }

  test("uses the standard AI stage in the level pipeline") {
    val moved = LevelState.pipeline().tick(level.ticking(LevelState.BetweenEnemySteps))

    assert(
      moved.enemies.map(enemy => enemy.kind -> enemy.position).toMap == Map(
        EnemyKind.Hunter -> Position(2, 1),
        EnemyKind.Anticipator -> Position(3, 4)
      )
    )
  }

  test("accepts a replacement strategy selection without changing the game loop") {
    val alwaysRight = new EnemyMovementStrategy:
      def nextMove(context: EnemyMovementContext): Option[Position] =
        Some(Position(context.enemyPosition.row, context.enemyPosition.col + 1))

    val moved = EnemyAiStage.stage(
      level.ticking(LevelState.BetweenEnemySteps),
      EnemyStrategySelection(_ => alwaysRight)
    )

    assert(moved.enemies.map(_.position).toSet == Set(Position(3, 2), Position(3, 4)))
  }

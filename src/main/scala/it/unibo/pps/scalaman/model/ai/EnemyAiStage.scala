package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.entities.Enemy
import it.unibo.pps.scalaman.model.{Direction, LevelState, Position}

/** Assigns the next movement to every idle enemy using its selected strategy. */
object EnemyAiStage:

  def stage(
      level: LevelState,
      selection: EnemyStrategySelection = EnemyStrategySelector
  ): LevelState =
    level.enemiesStepped(level.enemies.map(step(_, level, selection)))

  private def step(
      enemy: Enemy,
      level: LevelState,
      selection: EnemyStrategySelection
  ): Enemy =
    if enemy.entity.isMoving then enemy
    else
      selection
        .strategyFor(enemy.kind)
        .nextMove(
          EnemyMovementContext(
            enemyPosition = enemy.currentPos,
            teleportDisabled = enemy.previousPos.isDefined,
            playerPosition = level.player.currentPos,
            playerPreviousPosition = level.playerPreviousPos,
            map = level.maze
          )
        )
        .flatMap(directionTo(enemy.currentPos, _))
        .fold(enemy) { direction =>
          enemy.copy(previousPos = None).moving(_.move(direction, level.maze.isWalkable))
        }

  private def directionTo(from: Position, to: Position): Option[Direction] =
    Direction.values.find(from + _ == to)

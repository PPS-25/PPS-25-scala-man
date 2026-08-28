package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.{Direction, LevelState, Position}
import it.unibo.pps.scalaman.model.effects.Slowdown
import it.unibo.pps.scalaman.model.map.Enemy

/** Applies one AI decision to every enemy when enemies are due to step. Strategy selection is an
  * explicit dependency, so this stage can be reused with alternative selections without changing
  * the game loop.
  */
object EnemyAiStage:

  def stage(
      level: LevelState,
      selection: EnemyStrategySelection = EnemyStrategySelector
  )(using Slowdown): LevelState =
    if !level.enemyStepDue then level
    else level.enemiesStepped(level.enemies.map(step(_, level, selection)))

  private def step(
      enemy: Enemy,
      level: LevelState,
      selection: EnemyStrategySelection
  ): Enemy =
    selection
      .strategyFor(enemy.kind)
      .nextMove(
        EnemyMovementContext(
          enemyPosition = enemy.position,
          teleportDisabled = enemy.teleportDisabled,
          playerPosition = level.player.currentPos,
          playerPreviousPosition = level.playerPreviousPos,
          map = level.maze
        )
      )
      .flatMap(directionTo(enemy.position, _))
      .fold(enemy)(direction => enemy.copy(position = enemy.position + direction))

  private def directionTo(from: Position, to: Position): Option[Direction] =
    Direction.values.find(from + _ == to)

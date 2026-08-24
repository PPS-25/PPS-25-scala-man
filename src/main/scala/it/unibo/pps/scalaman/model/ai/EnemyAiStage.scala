package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.LevelState
import it.unibo.pps.scalaman.model.effects.Slowdown
import it.unibo.pps.scalaman.model.entities.Enemy

object EnemyAiStage:
  
  def stage(using Slowdown): LevelState => LevelState = 
    level => level.enemiesStepped(level.enemies.map(stepping(_, level)))
    
  private def stepping(enemy: Enemy, level: LevelState):
    Enemy = EnemyStrategySelector
    .strategyFor(enemy.kind)
    .nextMove(EnemyMovementContext(
      enemy.currentPos,
      level.player.currentPos,
      level.playerPreviousPos,
      level.maze
    ))
    .flatMap(enemy.currentPos.directionTo)
    .fold(enemy)(d => enemy.copy(entity = enemy.entity.move(d, level.maze.isWalkable)))
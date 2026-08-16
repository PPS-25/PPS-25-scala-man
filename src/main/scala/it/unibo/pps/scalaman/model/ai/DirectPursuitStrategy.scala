package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position

object DirectPursuitStrategy extends EnemyMovementStrategy:
  override def nextMove(context: EnemyMovementContext): Option[Position] =
    EnemyMovement.nextMoveToward(
      from = context.enemyPosition,
      target = context.playerPosition,
      map = context.map
    )

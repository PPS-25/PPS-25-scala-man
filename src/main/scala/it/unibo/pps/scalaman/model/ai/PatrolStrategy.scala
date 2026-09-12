package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position

object PatrolStrategy extends EnemyMovementStrategy:

  override def nextMove(context: EnemyMovementContext): Option[Position] =
    memoryAfter(context).flatMap: corner =>
      EnemyMovement.nextMoveToward(
        from = context.enemyPosition,
        target = corner,
        map = context.map,
        teleportDisabled = context.teleportDisabled
      )

  override def memoryAfter(context: EnemyMovementContext): Option[Position] =
    PatrolRoute.acrossCorners(context.map).map(headingOn(_, context))

  private def headingOn(route: PatrolRoute, context: EnemyMovementContext): Position =
    val chosen = context.enemyHeading.getOrElse(route.corners.head)
    if chosen != context.enemyPosition then chosen
    else route.nextAfter(chosen).getOrElse(route.corners.head)

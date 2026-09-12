package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.ValidatedMap

final case class EnemyMovementContext(
    enemyPosition: Position,
    teleportDisabled: Boolean,
    playerPosition: Position,
    playerPreviousPosition: Option[Position],
    map: ValidatedMap,
    enemyHeading: Option[Position] = None
)

trait EnemyMovementStrategy:
  def nextMove(context: EnemyMovementContext): Option[Position]

  def memoryAfter(context: EnemyMovementContext): Option[Position] = None

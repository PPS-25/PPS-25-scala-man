package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.ValidatedMap

/** Immutable data needed by an enemy strategy to choose its next movement. */
final case class EnemyMovementContext(
    enemyPosition: Position,
    teleportDisabled: Boolean,
    playerPosition: Position,
    playerPreviousPosition: Option[Position],
    map: ValidatedMap
)

/** Strategy abstraction for deterministic enemy movement decisions. */
trait EnemyMovementStrategy:
  def nextMove(context: EnemyMovementContext): Option[Position]

package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.ValidatedMap

import scala.annotation.tailrec

final case class PlayerAnticipationStrategy(stepsAhead: Int) extends EnemyMovementStrategy:
  require(stepsAhead >= 1, "stepsAhead must be positive")

  override def nextMove(context: EnemyMovementContext): Option[Position] =
    val predictedPosition = context.playerPreviousPosition.fold(context.playerPosition) {
      previous =>
        val delta = Delta(
          row = context.playerPosition.row - previous.row,
          col = context.playerPosition.col - previous.col
        ).unit

        if delta == Delta(0, 0) then context.playerPosition
        else predictPosition(context.playerPosition, delta, context.map)
    }

    EnemyMovement.firstStepTowards(
      from = context.enemyPosition,
      target = predictedPosition,
      map = context.map,
      canUseTeleport = context.canUseTeleport
    )

  private def predictPosition(
      position: Position,
      delta: Delta,
      map: ValidatedMap
  ): Position =
    @tailrec
    def advance(current: Position, remainingSteps: Int): Position =
      if remainingSteps == 0 then current
      else
        val next = Position(current.row + delta.row, current.col + delta.col)
        if map.isWalkable(next) then advance(next, remainingSteps - 1)
        else current

    advance(position, stepsAhead)

  private final case class Delta(row: Int, col: Int):
    def unit: Delta = Delta(row.sign, col.sign)

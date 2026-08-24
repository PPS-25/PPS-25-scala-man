package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.ValidatedMap

import scala.annotation.tailrec

final case class PlayerAnticipationStrategy(stepsAhead: Int) extends EnemyMovementStrategy:
  require(stepsAhead >= 1, "stepsAhead must be positive")

  override def nextMove(context: EnemyMovementContext): Option[Position] =
    val prediction = context.playerPreviousPosition.fold(Prediction(context.playerPosition, None)) {
      previous =>
        val delta = Delta(
          row = context.playerPosition.row - previous.row,
          col = context.playerPosition.col - previous.col
        ).unit

        if delta == Delta.Zero then Prediction(context.playerPosition, None)
        else predictPosition(context.playerPosition, delta, context.map)
    }

    nextMoveTowardPrediction(context.enemyPosition, prediction, context.map)

  private def nextMoveTowardPrediction(
      from: Position,
      prediction: Prediction,
      map: ValidatedMap
  ): Option[Position] =
    EnemyMovement
      .validMovesInOrder(from, map)
      .flatMap(position =>
        distanceToTarget(position, prediction.target, map).map(distance =>
          (position, distance, preferencePenalty(position, from, prediction.preferredDelta))
        )
      )
      .minByOption { case (_, distance, preferencePenalty) => (distance, preferencePenalty) }
      .map { case (position, _, _) => position }

  private def predictPosition(
      position: Position,
      delta: Delta,
      map: ValidatedMap
  ): Prediction =
    @tailrec
    def advance(current: Position, remainingSteps: Int): Prediction =
      if remainingSteps == 0 then Prediction(current, Some(delta))
      else
        val next = Position(current.row + delta.row, current.col + delta.col)
        if map.isWalkable(next) then advance(next, remainingSteps - 1)
        else Prediction(current, None)

    advance(position, stepsAhead)

  private def distanceToTarget(
      position: Position,
      target: Position,
      map: ValidatedMap
  ): Option[Int] =
    if position == target then Some(0)
    else EnemyMovement.shortestPath(position, target, map).map(_.size - 1)

  private def preferencePenalty(
      position: Position,
      from: Position,
      playerDelta: Option[Delta]
  ): Int =
    playerDelta.fold(0) { delta =>
      val enemyDelta = Delta(
        row = position.row - from.row,
        col = position.col - from.col
      )
      if enemyDelta == delta then 0 else 1
    }

  private final case class Prediction(target: Position, preferredDelta: Option[Delta])

  private final case class Delta(row: Int, col: Int):
    def unit: Delta = Delta(row.sign, col.sign)

  private object Delta:
    val Zero: Delta = Delta(0, 0)

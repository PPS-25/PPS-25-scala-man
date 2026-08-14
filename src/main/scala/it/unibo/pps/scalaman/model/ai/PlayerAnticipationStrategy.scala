package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position

final case class PlayerAnticipationStrategy(stepsAhead: Int) extends EnemyMovementStrategy:
  require(stepsAhead >= 1, "stepsAhead must be positive")

  override def nextMove(context: EnemyMovementContext): Option[Position] =
    val playerDelta = context.playerPreviousPosition.map(previous =>
      Delta(
        row = context.playerPosition.row - previous.row,
        col = context.playerPosition.col - previous.col
      )
    )
    val target = playerDelta.fold(context.playerPosition)(predictPosition(context.playerPosition, _))

    EnemyMovement
      .validMovesInOrder(context.enemyPosition, context.map)
      .minByOption(position =>
        (distance(position, target), preferencePenalty(position, context, playerDelta))
      )

  private def predictPosition(position: Position, delta: Delta): Position =
    Position(
      row = position.row + delta.row * stepsAhead,
      col = position.col + delta.col * stepsAhead
    )

  private def preferencePenalty(
      position: Position,
      context: EnemyMovementContext,
      playerDelta: Option[Delta]
  ): Int =
    playerDelta.fold(0) { delta =>
      val enemyDelta = Delta(
        row = position.row - context.enemyPosition.row,
        col = position.col - context.enemyPosition.col
      )
      if enemyDelta == delta.unit then 0 else 1
    }

  private def distance(left: Position, right: Position): Int =
    (left.row - right.row).abs + (left.col - right.col).abs

  private final case class Delta(row: Int, col: Int):
    def unit: Delta = Delta(row.sign, col.sign)

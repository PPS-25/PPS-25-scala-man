package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.Tile
import it.unibo.pps.scalaman.model.map.ValidatedMap

object EnemyMovement:
  def validMoves(from: Position, map: ValidatedMap): Set[Position] =
    validMovesInOrder(from, map).toSet

  private[ai] def nextMoveToward(
      from: Position,
      target: Position,
      map: ValidatedMap
  ): Option[Position] =
    validMovesInOrder(from, map).minByOption(distanceFrom(target))

  private[ai] def validMovesInOrder(from: Position, map: ValidatedMap): Vector[Position] =
    orthogonalNeighbors(from).filter(isWalkable(map, _))

  private def orthogonalNeighbors(position: Position): Vector[Position] =
    Vector(
      Position(position.row - 1, position.col),
      Position(position.row + 1, position.col),
      Position(position.row, position.col - 1),
      Position(position.row, position.col + 1)
    )

  private def isWalkable(map: ValidatedMap, position: Position): Boolean =
    position.row >= 0 &&
      position.row < map.raw.height &&
      position.col >= 0 &&
      position.col < map.raw.width &&
      map.raw.rows(position.row)(position.col) != Tile.Wall

  private def distanceFrom(target: Position)(position: Position): Int =
    (position.row - target.row).abs + (position.col - target.col).abs

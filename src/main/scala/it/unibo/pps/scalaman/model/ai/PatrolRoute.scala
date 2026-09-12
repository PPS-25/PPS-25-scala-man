package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.ValidatedMap

final case class PatrolRoute private (corners: Vector[Position]):
  require(corners.sizeIs >= 2, "a patrol route needs at least two corners")

  def nextAfter(corner: Position): Option[Position] =
    val index = corners.indexOf(corner)
    Option.when(index >= 0)(corners((index + 1) % corners.size))

object PatrolRoute:

  def acrossCorners(map: ValidatedMap): Option[PatrolRoute] =
    val stops = cornerCells(map).distinct
    Option.when(stops.sizeIs >= 2)(PatrolRoute(stops))

  private def cornerCells(map: ValidatedMap): Vector[Position] =
    val lastRow = map.raw.height - 1
    val lastCol = map.raw.width - 1
    Vector(
      Position(0, 0),
      Position(0, lastCol),
      Position(lastRow, lastCol),
      Position(lastRow, 0)
    ).flatMap(nearestWalkable(_, map))

  private def nearestWalkable(corner: Position, map: ValidatedMap): Option[Position] =
    map.raw.cells
      .filter(_.isWalkable)
      .minByOption(cell =>
        (distanceBetween(cell.position, corner), cell.position.row, cell.position.col)
      )
      .map(_.position)

private def distanceBetween(from: Position, to: Position): Int =
  math.abs(from.row - to.row) + math.abs(from.col - to.col)

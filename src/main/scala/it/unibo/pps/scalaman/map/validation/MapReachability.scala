package it.unibo.pps.scalaman.map.validation

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.RawMap

import scala.annotation.tailrec
import scala.collection.immutable.Queue

/** Traverses the walkable graph of a map, including its bidirectional teleport links. */
private[validation] object MapReachability:

  def from(
      spawn: Position,
      map: RawMap,
      teleports: Map[Int, (Position, Position)]
  ): Set[Position] =
    explore(map, teleportLinks(teleports), Queue(spawn), Set(spawn))

  @tailrec
  private def explore(
      map: RawMap,
      teleports: Map[Position, Position],
      frontier: Queue[Position],
      visited: Set[Position]
  ): Set[Position] =
    frontier.dequeueOption match
      case None                       => visited
      case Some((current, remaining)) =>
        val unseenNeighbors = neighbors(map, teleports, current).filterNot(visited.contains)
        explore(map, teleports, remaining.enqueueAll(unseenNeighbors), visited ++ unseenNeighbors)

  private def neighbors(
      map: RawMap,
      teleports: Map[Position, Position],
      position: Position
  ): Vector[Position] =
    orthogonalNeighbors(position).filter(map.isWalkable) ++ teleports.get(position)

  private def orthogonalNeighbors(position: Position): Vector[Position] =
    Vector(
      Position(position.row - 1, position.col),
      Position(position.row + 1, position.col),
      Position(position.row, position.col - 1),
      Position(position.row, position.col + 1)
    )

  private def teleportLinks(
      teleports: Map[Int, (Position, Position)]
  ): Map[Position, Position] =
    teleports.valuesIterator.flatMap { case (left, right) =>
      Iterator(left -> right, right -> left)
    }.toMap

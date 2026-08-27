package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.ValidatedMap

import scala.annotation.tailrec
import scala.collection.immutable.Queue

object EnemyMovement:
  def validMoves(from: Position, map: ValidatedMap): Set[Position] =
    validMoves(from, map, teleportDisabled = false)

  def validMoves(
      from: Position,
      map: ValidatedMap,
      teleportDisabled: Boolean
  ): Set[Position] =
    validMovesInOrder(from, map, teleportDisabled).toSet

  private[ai] def nextMoveToward(
      from: Position,
      target: Position,
      map: ValidatedMap,
      teleportDisabled: Boolean
  ): Option[Position] =
    shortestPath(from, target, map, teleportDisabled).flatMap(_.lift(1))

  private[ai] def validMovesInOrder(
      from: Position,
      map: ValidatedMap,
      teleportDisabled: Boolean = false
  ): Vector[Position] =
    orthogonalNeighbors(from).filter(isWalkable(map, _)) ++
      Option.when(!teleportDisabled)(teleportDestination(from, map)).flatten.toVector

  private def orthogonalNeighbors(position: Position): Vector[Position] =
    Vector(
      Position(position.row - 1, position.col),
      Position(position.row + 1, position.col),
      Position(position.row, position.col - 1),
      Position(position.row, position.col + 1)
    )

  private def isWalkable(map: ValidatedMap, position: Position): Boolean =
    map.raw.cellAt(position).exists(_.isWalkable)

  private def teleportDestination(from: Position, map: ValidatedMap): Option[Position] =
    map.teleports.valuesIterator.collectFirst {
      case (start, destination) if from == start       => destination
      case (start, destination) if from == destination => start
    }

  private[ai] def shortestPath(
      from: Position,
      target: Position,
      map: ValidatedMap,
      teleportDisabled: Boolean = false
  ): Option[Vector[Position]] =
    if from == target then None
    else explore(target, map, Queue(Vector(from)), Set(from), teleportDisabled)

  @tailrec
  private def explore(
      target: Position,
      map: ValidatedMap,
      frontier: Queue[Vector[Position]],
      visited: Set[Position],
      teleportDisabled: Boolean
  ): Option[Vector[Position]] =
    frontier.dequeueOption match
      case None                    => None
      case Some((path, remaining)) =>
        val current = path.last
        if current == target then Some(path)
        else
          val nextPositions =
            validMovesInOrder(
              current,
              map,
              teleportDisabled && path.size == 1
            ).filterNot(visited.contains)
          val nextPaths = nextPositions.map(position => path :+ position)
          explore(
            target,
            map,
            remaining.enqueueAll(nextPaths),
            visited ++ nextPositions,
            teleportDisabled
          )

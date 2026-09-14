package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.ValidatedMap

import scala.annotation.tailrec
import scala.collection.immutable.Queue

object EnemyMovement:
  def validMoves(from: Position, map: ValidatedMap): Set[Position] =
    validMoves(from, map, canUseTeleport = true)

  def validMoves(
      from: Position,
      map: ValidatedMap,
      canUseTeleport: Boolean
  ): Set[Position] =
    orderedMoves(from, map, canUseTeleport).toSet

  /** First position on a shortest path from `from` to `target`, when one exists. */
  private[ai] def firstStepTowards(
      from: Position,
      target: Position,
      map: ValidatedMap,
      canUseTeleport: Boolean
  ): Option[Position] =
    shortestPathTo(from, target, map, canUseTeleport).flatMap(_.lift(1))

  /** Walkable adjacent positions in the deterministic strategy tie-breaking order. */
  private[ai] def orderedMoves(
      from: Position,
      map: ValidatedMap,
      canUseTeleport: Boolean = true
  ): Vector[Position] =
    orthogonalNeighbors(from).filter(map.isWalkable) ++
      Option.when(canUseTeleport)(teleportDestination(from, map)).flatten.toVector

  private def orthogonalNeighbors(position: Position): Vector[Position] =
    Vector(
      Position(position.row - 1, position.col),
      Position(position.row + 1, position.col),
      Position(position.row, position.col - 1),
      Position(position.row, position.col + 1)
    )

  private def teleportDestination(from: Position, map: ValidatedMap): Option[Position] =
    map.teleports.valuesIterator.collectFirst {
      case (start, destination) if from == start       => destination
      case (start, destination) if from == destination => start
    }

  private[ai] def shortestPathTo(
      from: Position,
      target: Position,
      map: ValidatedMap,
      canUseTeleport: Boolean = true
  ): Option[Vector[Position]] =
    if from == target then None
    else explore(target, map, Queue(Vector(from)), Set(from), canUseTeleport)

  @tailrec
  private def explore(
      target: Position,
      map: ValidatedMap,
      frontier: Queue[Vector[Position]],
      visited: Set[Position],
      canUseTeleportAtOrigin: Boolean
  ): Option[Vector[Position]] =
    frontier.dequeueOption match
      case None                    => None
      case Some((path, remaining)) =>
        val current = path.last
        if current == target then Some(path)
        else
          val nextPositions =
            // An enemy that has just left a teleport must step away before using it again.
            orderedMoves(
              current,
              map,
              canUseTeleportAtOrigin || path.size > 1
            ).filterNot(visited.contains)
          val nextPaths = nextPositions.map(position => path :+ position)
          explore(
            target,
            map,
            remaining.enqueueAll(nextPaths),
            visited ++ nextPositions,
            canUseTeleportAtOrigin
          )

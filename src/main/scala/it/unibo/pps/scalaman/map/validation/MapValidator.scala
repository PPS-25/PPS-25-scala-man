package it.unibo.pps.scalaman.map.validation

import scala.annotation.tailrec
import scala.collection.immutable.Queue

import it.unibo.pps.scalaman.model.map.Cell
import it.unibo.pps.scalaman.model.map.Enemy
import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.map.MapValidationError
import it.unibo.pps.scalaman.model.map.Position
import it.unibo.pps.scalaman.model.map.RawMap
import it.unibo.pps.scalaman.model.map.ValidatedMap

object MapValidator:
  /** Validates a parsed map and enriches it with semantic information.
    *
    * This layer checks structural rules, teleport pairing, and graph reachability from the spawn
    * point, using teleports as bidirectional edges.
    */
  def validate(map: RawMap): Either[List[MapValidationError], ValidatedMap] =
    if hasInvalidDimensions(map) then
      Left(List(MapValidationError.InvalidDimensions(map.height, map.width)))
    else
      val inspection = inspect(map)
      val structuralErrors =
        requiredEntityErrors(inspection) ++
          spawnCountErrors(inspection.spawnPositions) ++
          unsupportedTeleportCodeErrors(inspection.teleportPositions)
      val teleportValidation = pairTeleports(inspection.teleportPositions)
      val allStructuralErrors = structuralErrors ++ teleportValidation.errors

      if allStructuralErrors.nonEmpty then Left(allStructuralErrors)
      else
        val reachable =
          reachablePositions(map, inspection.spawnPositions.head, teleportValidation.pairs)
        val reachabilityIssues = reachabilityProblems(inspection, reachable)

        if reachabilityIssues.nonEmpty then Left(reachabilityIssues)
        else
          Right(
            ValidatedMap(
              raw = map,
              spawn = inspection.spawnPositions.head,
              collectibles = inspection.collectibles.toSet,
              enemies = inspection.enemies.toSet,
              teleports = teleportValidation.pairs
            )
          )

  private def hasInvalidDimensions(map: RawMap): Boolean =
    map.height <= 0 || map.width <= 0 || map.rows.exists(_.length != map.width)

  private def inspect(map: RawMap): Inspection =
    map.rows.zipWithIndex.foldLeft(Inspection.empty) { case (inspection, (row, rowIndex)) =>
      row.zipWithIndex.foldLeft(inspection) { case (current, (cell, colIndex)) =>
        current.record(cell, Position(rowIndex, colIndex))
      }
    }

  private def requiredEntityErrors(inspection: Inspection): List[MapValidationError] =
    List(
      if inspection.collectibles.isEmpty then Some(MapValidationError.MissingCollectible) else None,
      if inspection.enemies.isEmpty then Some(MapValidationError.MissingEnemy) else None
    ).flatten

  private def spawnCountErrors(spawnPositions: Vector[Position]): List[MapValidationError] =
    spawnPositions.size match
      case 0     => List(MapValidationError.MissingSpawn)
      case 1     => Nil
      case count => List(MapValidationError.InvalidSpawnCount(count))

  private def pairTeleports(teleportPositions: Map[Int, Vector[Position]]): TeleportValidation =
    val results = (0 to 4).map(code => pairTeleport(code, teleportPositions))
    TeleportValidation(
      errors = results.flatMap(_.errors).toList,
      pairs = results.flatMap(_.pair).toMap
    )

  private def unsupportedTeleportCodeErrors(
      teleportPositions: Map[Int, Vector[Position]]
  ): List[MapValidationError] =
    // Defensive check for RawMap values constructed outside the parser.
    teleportPositions.keysIterator
      .filter(code => code < 0 || code > 9)
      .toVector
      .sorted
      .map(code => MapValidationError.UnsupportedTeleportCode(code))
      .toList

  private def pairTeleport(
      code: Int,
      teleportPositions: Map[Int, Vector[Position]]
  ): PairResult =
    val startPositions = teleportPositions.getOrElse(code, Vector.empty)
    val pairedPositions = teleportPositions.getOrElse(code + 5, Vector.empty)
    val occurrences = startPositions.size + pairedPositions.size

    if occurrences == 0 then PairResult.empty
    else if startPositions.size == 1 && pairedPositions.size == 1 then
      PairResult(Nil, Some(code -> (startPositions.head, pairedPositions.head)))
    else PairResult(List(MapValidationError.InvalidTeleportPair(code, occurrences)), None)

  private def reachabilityProblems(
      inspection: Inspection,
      reachable: Set[Position]
  ): List[MapValidationError] =
    unreachableCollectibles(inspection.collectibles, reachable)
      .map(position => MapValidationError.UnreachableCollectible(position))
      .toList ++
      unreachableEnemies(inspection.enemies, reachable)
        .map(enemy => MapValidationError.UnreachableEnemy(enemy.position))
        .toList

  private def unreachableCollectibles(
      collectibles: Vector[Position],
      reachable: Set[Position]
  ): Vector[Position] =
    collectibles.filterNot(reachable.contains).sortBy(position => (position.row, position.col))

  private def unreachableEnemies(enemies: Vector[Enemy], reachable: Set[Position]): Vector[Enemy] =
    enemies
      .filterNot(enemy => reachable.contains(enemy.position))
      .sortBy(enemy => (enemy.position.row, enemy.position.col))

  private def reachablePositions(
      map: RawMap,
      spawn: Position,
      teleports: Map[Int, (Position, Position)]
  ): Set[Position] =
    val teleportLinks = teleportLinksFrom(teleports)
    explore(map, teleportLinks, Queue(spawn), Set(spawn))

  @tailrec
  private def explore(
      map: RawMap,
      teleportLinks: Map[Position, Position],
      frontier: Queue[Position],
      visited: Set[Position]
  ): Set[Position] =
    frontier.dequeueOption match
      case None                       => visited
      case Some((current, remaining)) =>
        val nextPositions =
          adjacentPositions(map, current, teleportLinks).filterNot(visited.contains)
        explore(map, teleportLinks, remaining.enqueueAll(nextPositions), visited ++ nextPositions)

  private def adjacentPositions(
      map: RawMap,
      position: Position,
      teleportLinks: Map[Position, Position]
  ): Vector[Position] =
    orthogonalNeighbors(position).filter(isWalkable(map, _)) ++ teleportLinks.get(position)

  private def orthogonalNeighbors(position: Position): Vector[Position] =
    Vector(
      Position(position.row - 1, position.col),
      Position(position.row + 1, position.col),
      Position(position.row, position.col - 1),
      Position(position.row, position.col + 1)
    )

  private def isWalkable(map: RawMap, position: Position): Boolean =
    position.row >= 0 &&
      position.row < map.height &&
      position.col >= 0 &&
      position.col < map.width &&
      map.rows(position.row)(position.col) != Cell.Wall

  private def teleportLinksFrom(
      teleports: Map[Int, (Position, Position)]
  ): Map[Position, Position] =
    teleports.valuesIterator.flatMap { case (left, right) =>
      Iterator(left -> right, right -> left)
    }.toMap

  private final case class Inspection(
      spawnPositions: Vector[Position],
      collectibles: Vector[Position],
      enemies: Vector[Enemy],
      teleportPositions: Map[Int, Vector[Position]]
  ):
    def record(cell: Cell, position: Position): Inspection =
      cell match
        case Cell.Wall | Cell.Floor | Cell.InvulnerabilityBonus | Cell.SlowdownBonus => this
        case Cell.Spawn       => copy(spawnPositions = spawnPositions :+ position)
        case Cell.Collectible => copy(collectibles = collectibles :+ position)
        case Cell.Hunter      => copy(enemies = enemies :+ Enemy(position, EnemyKind.Hunter))
        case Cell.Anticipator => copy(enemies = enemies :+ Enemy(position, EnemyKind.Anticipator))
        case Cell.Teleport(code) =>
          copy(teleportPositions = teleportPositions.updatedWith(code)(appendPosition(position)))

  private object Inspection:
    val empty: Inspection = Inspection(Vector.empty, Vector.empty, Vector.empty, Map.empty)

  private final case class TeleportValidation(
      errors: List[MapValidationError],
      pairs: Map[Int, (Position, Position)]
  )
  private final case class PairResult(
      errors: List[MapValidationError],
      pair: Option[(Int, (Position, Position))]
  )

  private object PairResult:
    val empty: PairResult = PairResult(Nil, None)

  private def appendPosition(position: Position)(
      positions: Option[Vector[Position]]
  ): Option[Vector[Position]] =
    Some(positions.getOrElse(Vector.empty) :+ position)

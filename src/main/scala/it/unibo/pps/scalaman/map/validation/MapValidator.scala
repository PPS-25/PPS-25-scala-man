package it.unibo.pps.scalaman.map.validation

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.Tile
import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.map.MapCell
import it.unibo.pps.scalaman.model.map.MapValidationError
import it.unibo.pps.scalaman.model.map.RawMap
import it.unibo.pps.scalaman.model.map.ValidatedMap
import it.unibo.pps.scalaman.model.map.EnemySpawn

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
          unsupportedTeleportCodeErrors(inspection.teleportPositions) ++
          openBorderErrors(map)
      val teleportValidation = pairTeleports(inspection.teleportPositions)
      val allStructuralErrors = structuralErrors ++ teleportValidation.errors

      inspection.spawnPositions match
        case Vector(spawn) if allStructuralErrors.isEmpty =>
          val reachable = MapReachability.from(
            spawn = spawn,
            map = map,
            teleports = teleportValidation.pairs
          )
          val reachabilityIssues = reachabilityProblems(inspection, reachable)

          if reachabilityIssues.nonEmpty then Left(reachabilityIssues)
          else
            Right(
              ValidatedMap(
                raw = map,
                spawn = spawn,
                collectibles = inspection.collectibles.toSet,
                enemies = inspection.enemies.toSet,
                teleports = teleportValidation.pairs
              )
            )
        case _ => Left(allStructuralErrors)

  private def hasInvalidDimensions(map: RawMap): Boolean =
    map.height <= 0 || map.width <= 0 || map.rows.exists(_.length != map.width)

  private def inspect(map: RawMap): Inspection =
    map.cells.foldLeft(Inspection.empty)((inspection, cell) => inspection.record(cell))

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
    else
      (startPositions, pairedPositions) match
        case (Vector(start), Vector(paired)) => PairResult(Nil, Some(code -> (start, paired)))
        case _ => PairResult(List(MapValidationError.InvalidTeleportPair(code, occurrences)), None)

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

  private def unreachableEnemies(
      enemies: Vector[EnemySpawn],
      reachable: Set[Position]
  ): Vector[EnemySpawn] =
    enemies
      .filterNot(enemy => reachable.contains(enemy.position))
      .sortBy(enemy => (enemy.position.row, enemy.position.col))

  private final case class Inspection(
      spawnPositions: Vector[Position],
      collectibles: Vector[Position],
      enemies: Vector[EnemySpawn],
      teleportPositions: Map[Int, Vector[Position]]
  ):
    def record(cell: MapCell): Inspection =
      cell.tile match
        case Tile.Wall | Tile.Floor | Tile.InvulnerabilityBonus | Tile.SlowdownBonus => this
        case Tile.Spawn       => copy(spawnPositions = spawnPositions :+ cell.position)
        case Tile.Collectible => copy(collectibles = collectibles :+ cell.position)
        case Tile.Hunter => copy(enemies = enemies :+ EnemySpawn(cell.position, EnemyKind.Hunter))
        case Tile.Anticipator =>
          copy(enemies = enemies :+ EnemySpawn(cell.position, EnemyKind.Anticipator))
        case Tile.Patroller =>
          copy(enemies = enemies :+ EnemySpawn(cell.position, EnemyKind.Patroller))
        case Tile.Teleport(code) =>
          copy(teleportPositions =
            teleportPositions.updatedWith(code)(appendPosition(cell.position))
          )

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

  private def openBorderPositions(map: RawMap): Set[Position] =
    (for
      row <- 0 until map.height
      col <- 0 until map.width
      if row == 0 || row == map.height - 1 || col == 0 || col == map.width - 1
      if map.rows(row)(col) != Tile.Wall
    yield Position(row, col)).toSet

  private def openBorderErrors(map: RawMap): List[MapValidationError] =
    val openings = openBorderPositions(map)
    if openings.nonEmpty
    then List(MapValidationError.OpenBorder(openings))
    else Nil

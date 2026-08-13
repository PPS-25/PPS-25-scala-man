package it.unibo.pps.scalaman.map.generator

import it.unibo.pps.scalaman.model.Position
import scala.util.Random
import it.unibo.pps.scalaman.model.map.Tile
import it.unibo.pps.scalaman.model.map.MapGenerationError
import it.unibo.pps.scalaman.model.map.MapGenerationSpec
import it.unibo.pps.scalaman.model.map.RawMap

object MapGenerator:
  private val teleportCodePairs: Vector[(Int, Int)] = Vector(0 -> 5, 1 -> 6, 2 -> 7, 3 -> 8, 4 -> 9)

  /** Generates a random-but-valid raw map from a generation specification.
    *
    * The map is always rectangular, framed by walls, and populated only with cells that can later
    * be parsed and validated by the map pipeline.
    */
  def generate(spec: MapGenerationSpec): Either[List[MapGenerationError], RawMap] =
    val errors = specificationErrors(spec)

    if errors.nonEmpty then Left(errors)
    else Right(buildMap(spec))

  private def specificationErrors(spec: MapGenerationSpec): List[MapGenerationError] =
    List(
      if spec.width < 3 then Some(invalid("width must be at least 3")) else None,
      if spec.height < 3 then Some(invalid("height must be at least 3")) else None,
      if spec.collectibles <= 0 then Some(invalid("collectibles must be positive")) else None,
      if spec.enemies <= 0 then Some(invalid("enemies must be positive")) else None,
      if spec.teleports < 0 then Some(invalid("teleports cannot be negative")) else None,
      if spec.teleports > teleportCodePairs.size then
        Some(invalid("at most 5 teleport pairs are supported"))
      else None,
      if requiredCells(spec) > interiorCapacity(spec) then
        Some(invalid("specification does not fit in the available interior cells"))
      else None
    ).flatten

  private def requiredCells(spec: MapGenerationSpec): Int =
    1 + spec.collectibles + spec.enemies + spec.teleports * 2

  private def interiorCapacity(spec: MapGenerationSpec): Int =
    math.max(0, spec.width - 2) * math.max(0, spec.height - 2)

  private def buildMap(spec: MapGenerationSpec): RawMap =
    val positions = shuffledInteriorPositions(spec)
    val layout = placeEntities(spec, positions)
    RawMap(layout)

  private def shuffledInteriorPositions(spec: MapGenerationSpec): Vector[Position] =
    val rng = spec.seed.fold(new Random())(seed => new Random(seed))
    rng.shuffle(interiorPositions(spec.width, spec.height)).toVector

  private def interiorPositions(width: Int, height: Int): Vector[Position] =
    (for
      row <- 1 until height - 1
      col <- 1 until width - 1
    yield Position(row, col)).toVector

  private def placeEntities(
      spec: MapGenerationSpec,
      positions: Vector[Position]
  ): Vector[Vector[Tile]] =
    val placements = computePlacements(spec, positions)
    val base = buildBaseGrid(spec.width, spec.height)
    overlay(base, placements.cells)

  private def computePlacements(spec: MapGenerationSpec, positions: Vector[Position]): Placements =
    val spawnPosition = positions.head
    val collectiblePositions = positions.slice(1, 1 + spec.collectibles)
    val enemyPositions =
      positions.slice(1 + spec.collectibles, 1 + spec.collectibles + spec.enemies)
    val teleportPositions = positions.drop(1 + spec.collectibles + spec.enemies)
    val teleportCells = teleportCellsFor(spec.teleports, teleportPositions)

    Placements(
      cells = Vector(spawnPosition -> Tile.Spawn) ++
        collectiblePositions.map(_ -> Tile.Collectible) ++
        enemyPlacements(enemyPositions) ++
        teleportCells
    )

  private def enemyPlacements(positions: Vector[Position]): Vector[(Position, Tile)] =
    positions.zipWithIndex.map { case (position, index) =>
      position -> enemyCell(index)
    }

  private def enemyCell(index: Int): Tile =
    if index % 2 == 0 then Tile.Hunter else Tile.Anticipator

  private def teleportCellsFor(
      teleports: Int,
      positions: Vector[Position]
  ): Vector[(Position, Tile)] =
    val positionPairs = positions.grouped(2).toVector
    val codePairs = teleportCodePairs.take(teleports)

    positionPairs.zip(codePairs).flatMap { case (positionsPair, (startCode, pairedCode)) =>
      positionsPair match
        case Vector(first, second) =>
          Vector(first -> Tile.Teleport(startCode), second -> Tile.Teleport(pairedCode))
        case _ =>
          Vector.empty
    }

  private def buildBaseGrid(width: Int, height: Int): Vector[Vector[Tile]] =
    Vector.tabulate(height, width) { (row, col) =>
      if isBorder(row, col, width, height) then Tile.Wall else Tile.Floor
    }

  private def isBorder(row: Int, col: Int, width: Int, height: Int): Boolean =
    row == 0 || col == 0 || row == height - 1 || col == width - 1

  private def overlay(
      grid: Vector[Vector[Tile]],
      cells: Vector[(Position, Tile)]
  ): Vector[Vector[Tile]] =
    cells.foldLeft(grid) { case (currentGrid, (position, cell)) =>
      currentGrid.updated(position.row, currentGrid(position.row).updated(position.col, cell))
    }

  private def invalid(reason: String): MapGenerationError =
    MapGenerationError.InvalidSpecification(reason)

  private final case class Placements(cells: Vector[(Position, Tile)])

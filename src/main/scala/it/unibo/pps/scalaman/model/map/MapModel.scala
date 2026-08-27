package it.unibo.pps.scalaman.model.map

import it.unibo.pps.scalaman.model.Position

import java.nio.file.Path

enum Tile:
  case Wall, Floor, Spawn, Collectible
  case Hunter, Anticipator
  case InvulnerabilityBonus, SlowdownBonus
  case Teleport(code: Int)

final case class MapCell(position: Position, tile: Tile):
  def isWalkable: Boolean = tile != Tile.Wall

enum EnemyKind:
  case Hunter, Anticipator

final case class Enemy(
    position: Position,
    kind: EnemyKind,
    teleportDisabled: Boolean = false
)

final case class RawMap(rows: Vector[Vector[Tile]]):
  val height: Int = rows.length
  val width: Int = rows.headOption.fold(0)(_.length)
  val cells: Vector[MapCell] =
    rows.zipWithIndex.flatMap { case (row, rowIndex) =>
      row.zipWithIndex.map { case (tile, colIndex) =>
        MapCell(Position(rowIndex, colIndex), tile)
      }
    }

  def cellAt(position: Position): Option[MapCell] =
    Option.when(
      position.row >= 0 &&
        position.row < height &&
        position.col >= 0 &&
        position.col < width
    )(MapCell(position, rows(position.row)(position.col)))

final case class ValidatedMap(
    raw: RawMap,
    spawn: Position,
    collectibles: Set[Position],
    enemies: Set[Enemy],
    teleports: Map[Int, (Position, Position)]
)

sealed trait MapLoadError
object MapLoadError:
  final case class FileNotFound(path: Path) extends MapLoadError
  final case class ReadFailed(path: Path, message: String) extends MapLoadError

sealed trait MapParseError
object MapParseError:
  case object EmptyMap extends MapParseError
  final case class RaggedRow(rowIndex: Int, expectedWidth: Int, actualWidth: Int)
      extends MapParseError
  final case class UnsupportedSymbol(symbol: Char, row: Int, col: Int) extends MapParseError

sealed trait MapValidationError
object MapValidationError:
  final case class InvalidDimensions(height: Int, width: Int) extends MapValidationError
  case object MissingSpawn extends MapValidationError
  final case class InvalidSpawnCount(count: Int) extends MapValidationError
  case object MissingCollectible extends MapValidationError
  case object MissingEnemy extends MapValidationError
  final case class UnreachableCollectible(position: Position) extends MapValidationError
  final case class UnreachableEnemy(position: Position) extends MapValidationError
  final case class InvalidTeleportPair(code: Int, occurrences: Int) extends MapValidationError
  final case class UnsupportedTeleportCode(code: Int) extends MapValidationError
  final case class OpenBorder(positions: Set[Position]) extends MapValidationError

sealed trait MapGenerationError
object MapGenerationError:
  final case class InvalidSpecification(reason: String) extends MapGenerationError

final case class MapGenerationSpec(
    width: Int,
    height: Int,
    collectibles: Int,
    teleports: Int,
    enemies: Int = 1,
    seed: Option[Long] = None
)

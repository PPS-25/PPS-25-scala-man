package it.unibo.pps.scalaman.model.map

import java.nio.file.Path

final case class Position(row: Int, col: Int)

enum Cell:
  case Wall, Floor, Spawn, Collectible
  case Hunter, Anticipator
  case InvulnerabilityBonus, SlowdownBonus
  case Teleport(code: Int)

enum EnemyKind:
  case Hunter, Anticipator

final case class Enemy(position: Position, kind: EnemyKind)

final case class RawMap(rows: Vector[Vector[Cell]]):
  val height: Int = rows.length
  val width: Int = rows.headOption.fold(0)(_.length)

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

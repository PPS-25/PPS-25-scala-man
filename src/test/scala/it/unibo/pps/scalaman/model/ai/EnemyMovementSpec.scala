package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.RawMap
import it.unibo.pps.scalaman.model.map.Tile
import it.unibo.pps.scalaman.model.map.ValidatedMap
import org.scalatest.funsuite.AnyFunSuite

class EnemyMovementSpec extends AnyFunSuite:
  test("selects only valid orthogonal movement destinations") {
    val map = validatedMap(
      Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
      )
    )

    val moves = EnemyMovement.validMoves(Position(2, 2), map)

    assert(moves == Set(Position(1, 2), Position(2, 1), Position(3, 2)))
  }

  test("ignores positions outside the map") {
    val map = validatedMap(
      Vector(
        Vector(Tile.Floor, Tile.Floor),
        Vector(Tile.Floor, Tile.Wall)
      )
    )

    val moves = EnemyMovement.validMoves(Position(0, 0), map)

    assert(moves == Set(Position(1, 0), Position(0, 1)))
  }

  test("returns no valid movement when every orthogonal destination is blocked") {
    val map = validatedMap(
      Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall)
      )
    )

    assert(EnemyMovement.validMoves(Position(1, 1), map).isEmpty)
  }

  test("treats non-wall tiles as walkable destinations") {
    val map = validatedMap(
      Vector(
        Vector(Tile.Wall, Tile.Collectible, Tile.Wall),
        Vector(Tile.Hunter, Tile.Floor, Tile.Anticipator),
        Vector(Tile.Wall, Tile.InvulnerabilityBonus, Tile.Wall)
      )
    )

    val moves = EnemyMovement.validMoves(Position(1, 1), map)

    assert(
      moves == Set(
        Position(0, 1),
        Position(1, 0),
        Position(1, 2),
        Position(2, 1)
      )
    )
  }

  test("adds the paired teleport destination to valid movement destinations") {
    val teleportStart = Position(1, 1)
    val teleportDestination = Position(1, 3)
    val map = validatedMap(
      rows = Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(
          Tile.Wall,
          Tile.Teleport(0),
          Tile.Floor,
          Tile.Teleport(5),
          Tile.Wall
        ),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
      ),
      teleports = Map(0 -> (teleportStart, teleportDestination))
    )

    val moves = EnemyMovement.validMoves(teleportStart, map)

    assert(moves == Set(Position(1, 2), teleportDestination))
  }

  private def validatedMap(
      rows: Vector[Vector[Tile]],
      teleports: Map[Int, (Position, Position)] = Map.empty
  ): ValidatedMap =
    ValidatedMap(
      raw = RawMap(rows),
      spawn = Position(0, 0),
      collectibles = Set.empty,
      enemies = Set.empty,
      teleports = teleports
    )
end EnemyMovementSpec

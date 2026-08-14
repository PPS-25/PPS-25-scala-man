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

  private def validatedMap(rows: Vector[Vector[Tile]]): ValidatedMap =
    ValidatedMap(
      raw = RawMap(rows),
      spawn = Position(0, 0),
      collectibles = Set.empty,
      enemies = Set.empty,
      teleports = Map.empty
    )
end EnemyMovementSpec

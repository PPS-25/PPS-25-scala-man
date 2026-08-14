package it.unibo.pps.scalaman.model.map

import it.unibo.pps.scalaman.model.Position
import org.scalatest.funsuite.AnyFunSuite

class MapCellSpec extends AnyFunSuite:
  test("keeps together a position and its tile") {
    val cell = MapCell(Position(row = 2, col = 3), Tile.Collectible)

    assert(cell.position == Position(row = 2, col = 3))
    assert(cell.tile == Tile.Collectible)
  }

  test("marks walls as not walkable") {
    val cell = MapCell(Position(row = 1, col = 1), Tile.Wall)

    assert(!cell.isWalkable)
  }

  test("marks non-wall tiles as walkable") {
    val walkableTiles = List(
      Tile.Floor,
      Tile.Spawn,
      Tile.Collectible,
      Tile.Hunter,
      Tile.Anticipator,
      Tile.InvulnerabilityBonus,
      Tile.SlowdownBonus,
      Tile.Teleport(code = 0)
    )

    assert(walkableTiles.forall(tile => MapCell(Position(row = 0, col = 0), tile).isWalkable))
  }

  test("raw maps return positioned cells at valid positions") {
    val map = RawMap(
      Vector(
        Vector(Tile.Wall, Tile.Floor),
        Vector(Tile.Spawn, Tile.Collectible)
      )
    )

    assert(
      map
        .cellAt(Position(row = 1, col = 0))
        .contains(MapCell(Position(row = 1, col = 0), Tile.Spawn))
    )
  }

  test("raw maps return no cell outside their bounds") {
    val map = RawMap(Vector(Vector(Tile.Floor)))

    assert(map.cellAt(Position(row = -1, col = 0)).isEmpty)
    assert(map.cellAt(Position(row = 0, col = -1)).isEmpty)
    assert(map.cellAt(Position(row = 1, col = 0)).isEmpty)
    assert(map.cellAt(Position(row = 0, col = 1)).isEmpty)
  }

  test("raw maps enumerate all positioned cells in row order") {
    val map = RawMap(
      Vector(
        Vector(Tile.Wall, Tile.Floor),
        Vector(Tile.Spawn, Tile.Collectible)
      )
    )

    assert(
      map.cells == Vector(
        MapCell(Position(row = 0, col = 0), Tile.Wall),
        MapCell(Position(row = 0, col = 1), Tile.Floor),
        MapCell(Position(row = 1, col = 0), Tile.Spawn),
        MapCell(Position(row = 1, col = 1), Tile.Collectible)
      )
    )
  }
end MapCellSpec

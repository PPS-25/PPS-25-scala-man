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
end MapCellSpec

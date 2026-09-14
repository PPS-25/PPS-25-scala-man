package it.unibo.pps.scalaman.map.validation

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.{RawMap, Tile}
import org.scalatest.funsuite.AnyFunSuite

class MapReachabilitySpec extends AnyFunSuite:

  test("walks through a teleport but never through walls") {
    val entrance = Position(1, 2)
    val exit = Position(3, 4)
    val map = RawMap(
      Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Spawn, Tile.Teleport(0), Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Teleport(5), Tile.Collectible),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
      )
    )

    val reachable = MapReachability.from(
      spawn = Position(1, 1),
      map = map,
      teleports = Map(0 -> (entrance, exit))
    )

    assert(reachable.contains(Position(3, 5)))
    assert(!reachable.contains(Position(2, 2)))
  }

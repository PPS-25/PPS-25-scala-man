package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.{RawMap, Tile, ValidatedMap}

/** Builds mazes out of an ASCII layout, where `#` is a wall and anything else is walkable. */
trait MazeLayouts:

  protected def maze(layout: String): ValidatedMap =
    val rows = layout.linesIterator
      .map(_.map(symbol => if symbol == '#' then Tile.Wall else Tile.Floor).toVector)
      .toVector
    ValidatedMap(
      raw = RawMap(rows),
      spawn = Position(1, 1),
      collectibles = Set.empty,
      enemies = Set.empty,
      teleports = Map.empty
    )

  protected val openRoom: ValidatedMap = maze(
    """#######
      |#.....#
      |#.....#
      |#.....#
      |#######""".stripMargin
  )

  protected val roomWithInnerWall: ValidatedMap = maze(
    """#####
      |#...#
      |#.#.#
      |#...#
      |#####""".stripMargin
  )

  protected val deadEndCorridors: ValidatedMap = maze(
    """######
      |#....#
      |####.#
      |#....#
      |######""".stripMargin
  )

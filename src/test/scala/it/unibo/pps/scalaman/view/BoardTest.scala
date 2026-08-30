package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.LevelTestSupport.{
  maze,
  mazeWithTeleports,
  teleportDestination,
  teleportStart
}
import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.map.ValidatedMap
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

class BoardTest extends AnyFunSuite:

  private val board = Board.of(maze)

  /** The furthest teleport pair a map may hold, the last one to be given a look of its own. */
  private def furthestTeleports: ValidatedMap =
    val text = Files.readString(
      Paths.get(getClass.getResource("/maps/valid/teleport-pairs/4-9.txt").toURI),
      StandardCharsets.UTF_8
    )
    MapParser.parse(text).flatMap(MapValidator.validate).toOption.get

  test("a wall is drawn where the maze has one") {
    assert(board.at(Position(0, 0)).contains(Sprite.Wall))
  }

  test("a floor is drawn where the maze has one") {
    assert(board.at(Position(1, 2)).contains(Sprite.Floor))
  }

  test("what only marks a starting place is drawn as floor") {
    val markers = Set(Position(1, 1), Position(1, 3), Position(1, 5), Position(3, 1))
    assert(markers.flatMap(board.at) == Set(Sprite.Floor))
  }

  test("a teleport is drawn, being part of the maze") {
    assert(Board.of(mazeWithTeleports).at(teleportStart).contains(Sprite.Teleport(0)))
  }

  test("both ends of a teleport are drawn alike, to tell where whoever enters comes out") {
    val teleports = Board.of(mazeWithTeleports)
    assert(teleports.at(teleportStart) == teleports.at(teleportDestination))
  }

  test("the board is as large as the maze") {
    assert((board.height, board.width) == (maze.raw.height, maze.raw.width))
  }

  test("nothing is drawn outside the maze") {
    assert(board.at(Position(-1, 0)).isEmpty)
  }

  test("every door a map is allowed to hold is one the game knows how to draw") {
    val drawn = Board.of(furthestTeleports).cells.flatten.toSet
    assert(drawn.subsetOf(Sprite.All))
  }

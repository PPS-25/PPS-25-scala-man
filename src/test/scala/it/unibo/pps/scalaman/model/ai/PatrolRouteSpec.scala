package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import org.scalatest.funsuite.AnyFunSuite

class PatrolRouteSpec extends AnyFunSuite with MazeLayouts:

  test("the corners are the walkable cells nearest the four map corners, clockwise") {
    val route = PatrolRoute.acrossCorners(openRoom)

    assert(
      route.map(_.corners) ==
        Some(Vector(Position(1, 1), Position(1, 5), Position(3, 5), Position(3, 1)))
    )
  }

  test("the corner after the last one is the first one") {
    val route = PatrolRoute.acrossCorners(openRoom)

    assert(route.flatMap(r => r.nextAfter(r.corners.last)) == route.map(_.corners.head))
  }

  test("a cell that is not a corner has no corner after it") {
    assert(PatrolRoute.acrossCorners(openRoom).flatMap(_.nextAfter(Position(2, 3))).isEmpty)
  }

  test("corners are found around a wall standing in the way") {
    val corners = PatrolRoute.acrossCorners(roomWithInnerWall).fold(Vector.empty)(_.corners)

    assert(corners == Vector(Position(1, 1), Position(1, 3), Position(3, 3), Position(3, 1)))
  }

  test("a maze of dead ends still has corners to patrol") {
    assert(PatrolRoute.acrossCorners(deadEndCorridors).isDefined)
  }

  test("a map with no walkable cell has no corners to patrol") {
    assert(PatrolRoute.acrossCorners(maze("###\n###\n###")).isEmpty)
  }
end PatrolRouteSpec

package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.ValidatedMap
import org.scalatest.funsuite.AnyFunSuite

import scala.annotation.tailrec

class PatrolStrategySpec extends AnyFunSuite with MazeLayouts:

  private val corners = PatrolRoute.acrossCorners(openRoom).get.corners

  test("an enemy that is heading nowhere makes for the first corner, near or far") {
    val context = contextAt(Position(3, 4), openRoom)

    assert(PatrolStrategy.memoryAfter(context).contains(corners.head))
  }

  test("an enemy on its way keeps heading for the same corner") {
    val context = contextAt(Position(1, 3), openRoom, heading = Some(Position(1, 5)))

    assert(PatrolStrategy.memoryAfter(context).contains(Position(1, 5)))
  }

  test("an enemy standing on the corner it was heading for takes the next one") {
    val context = contextAt(Position(1, 1), openRoom, heading = Some(Position(1, 1)))

    assert(PatrolStrategy.memoryAfter(context).contains(Position(1, 5)))
  }

  test("an enemy that starts on the first corner makes for the next one, not for itself") {
    val context = contextAt(corners.head, openRoom)

    assert(PatrolStrategy.memoryAfter(context).contains(corners(1)))
  }

  test("the move is a step towards the corner it is heading for") {
    val context = contextAt(Position(1, 3), openRoom, heading = Some(Position(1, 5)))

    assert(PatrolStrategy.nextMove(context).contains(Position(1, 4)))
  }

  test("where it goes does not depend on the player") {
    val standing = contextAt(Position(1, 3), openRoom, heading = Some(Position(1, 5)))
    val chased = standing.copy(
      playerPosition = Position(3, 1),
      playerPreviousPosition = Some(Position(3, 2))
    )

    assert(PatrolStrategy.nextMove(standing) == PatrolStrategy.nextMove(chased))
  }

  test("the four corners are touched one after the other, clockwise, over and over") {
    val touched = patrol(from = corners.head, openRoom, steps = 60)

    assert(touched == LazyList.continually(corners).flatten.take(touched.size).toVector)
  }

  private def patrol(from: Position, map: ValidatedMap, steps: Int): Vector[Position] =
    @tailrec
    def walk(
        at: Position,
        heading: Option[Position],
        left: Int,
        touched: Vector[Position]
    ): Vector[Position] =
      if left == 0 then touched
      else
        val context = contextAt(at, map, heading)
        val seen = if corners.contains(at) && !touched.lastOption.contains(at) then touched :+ at
        else touched
        PatrolStrategy.nextMove(context) match
          case None       => seen
          case Some(next) => walk(next, PatrolStrategy.memoryAfter(context), left - 1, seen)

    walk(from, None, steps, Vector.empty)

  private def contextAt(
      enemyPosition: Position,
      map: ValidatedMap,
      heading: Option[Position] = None
  ): EnemyMovementContext =
    EnemyMovementContext(
      enemyPosition = enemyPosition,
      teleportDisabled = false,
      playerPosition = Position(2, 2),
      playerPreviousPosition = None,
      map = map,
      enemyHeading = heading
    )
end PatrolStrategySpec

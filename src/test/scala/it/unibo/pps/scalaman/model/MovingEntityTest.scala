package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Direction.{Down, Left, Right, Up}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class MovingEntityTest extends AnyFunSuite:
  private val millis = 100.millis
  private val startingCell = Cell(0, 0)

  private val basicEntity = MovingEntity(
    startingCell,
    Left,
    millis,
    Some(Movement(startingCell, startingCell + Left, millis))
  )
  test("a moving entity that is not moving stays in its starting cell") {
    assert(basicEntity.currentCell == startingCell)
  }

  test(
    "a moving entity given a Movement correctly moves in the 4 directions Up, Down, Left, Right"
  ) {
    val expectedTargets = Seq(
      Down -> Cell(0, +1),
      Up -> Cell(0, -1),
      Left -> Cell(-1, 0),
      Right -> Cell(1, 0)
    )
    expectedTargets
      .foreach { case (direction, target) =>
        val entity = basicEntity.move(direction)
        assert(entity.movement.contains(Movement(startingCell, target, millis)))
        assert(entity.facing === direction)
      }
  }

  test("face changes the direction the entity is facing") {
    assert(basicEntity.face(Left).facing == Left)
  }

  test("face does not change the movement in progress") {
    assert(
      basicEntity.move(Down).face(Left).movement == basicEntity
        .move(Down)
        .movement
    )
    assert(
      basicEntity.move(Down).face(Left).currentCell == basicEntity
        .move(Down)
        .currentCell
    )
  }

  test("update does nothing when the entity is not moving") {
    val unmovingEntity = MovingEntity(startingCell, Left, millis, None)
    assert(unmovingEntity.update(200.millis).movement.isEmpty)
  }

  test(
    "update reduces the remaining time by the given amount when the movement is incomplete"
  ) {
    assert(
      basicEntity
        .update(50.millis)
        .movement
        .get
        .remaining == basicEntity.movement.get.remaining - 50.millis
    )
  }

  test(
    "update makes the entity stop moving if the remaining time reaches zero"
  ) {
    assert(
      basicEntity.update(basicEntity.movement.get.remaining).movement.isEmpty
    )
  }

package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Direction.{Down, Left, Right, Up}
import org.scalatest.funsuite.AnyFunSuite
import scala.concurrent.duration.DurationInt

class MovingEntityTest extends AnyFunSuite:
  private val millis = 100.millis
  private val startingPos = Position(0, 0)

  private val basicEntity = MovingEntity(
    startingPos,
    Left,
    millis,
    Some(Movement(startingPos, startingPos + Left, millis))
  )
  test("a moving entity that is not moving stays in its starting cell") {
    assert(basicEntity.currentPos == startingPos)
  }

  test(
    "a moving entity given a Movement correctly moves in the 4 directions Up, Down, Left, Right"
  ) {
    val expectedTargets = Seq(
      Down -> Position(+1, 0),
      Up -> Position(-1, 0),
      Left -> Position(0, -1),
      Right -> Position(0, 1)
    )
    expectedTargets
      .foreach { case (direction, target) =>
        val entity = basicEntity.move(direction)
        assert(entity.movement.contains(Movement(startingPos, target, millis)))
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
      basicEntity.move(Down).face(Left).currentPos == basicEntity
        .move(Down)
        .currentPos
    )
  }

  test("update does nothing when the entity is not moving") {
    val unmovingEntity = MovingEntity(startingPos, Left, millis, None)
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

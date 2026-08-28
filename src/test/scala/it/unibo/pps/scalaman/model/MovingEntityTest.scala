package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Direction.{Down, Left, Right, Up}
import it.unibo.pps.scalaman.model.entities.MovingEntity
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
        val entity = basicEntity.move(direction, _ => true)
        assert(entity.movement.contains(Movement(startingPos, target, millis)))
        assert(entity.facing === direction)
      }
  }

  test("face changes the direction the entity is facing") {
    assert(basicEntity.face(Left).facing == Left)
  }

  test("face does not change the movement in progress") {
    assert(
      basicEntity.move(Down, _ => true).face(Left).movement == basicEntity
        .move(Down, _ => true)
        .movement
    )
    assert(
      basicEntity.move(Down, _ => true).face(Left).currentPos == basicEntity
        .move(Down, _ => true)
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

  test("two entities on the same cell meet") {
    assert(basicEntity.meets(MovingEntity(basicEntity.currentPos, Direction.Right, 250.millis)))
  }

  test("two entities exchanging cells meet") {
    val positionOne = Position(0, 0)
    val positionTwo = Position(0, 1)
    val entityOne = MovingEntity(
      positionOne,
      Right,
      100.millis,
      Some(Movement(positionOne, positionTwo, 1.millis))
    )
    val entityTwo = MovingEntity(
      positionTwo,
      Left,
      100.millis,
      Some(Movement(positionTwo, positionOne, 1.millis))
    )
    assert(entityOne.meets(entityTwo))
  }

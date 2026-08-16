package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.map.io.MapLoader
import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.map.{MapTestSupport, ValidatedMap}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class MovingEntityMapIT extends AnyFunSuite, MapTestSupport:
  private val validatedMap: ValidatedMap =
    val validated = MapLoader.load(resourcePath("valid/basic-map.txt")) match
      case Right(text) =>
        MapParser.parse(text) match
          case Right(parsed) => MapValidator.validate(parsed)
          case Left(errors)  => fail(s"$errors")
      case Left(error) => fail(s"$error")
    validated.getOrElse(fail(s"$validated"))

  private val isWalkable: Position => Boolean = MapValidator.isWalkable(validatedMap.raw, _)
  private val spawn = validatedMap.spawn
  private val entity = MovingEntity(spawn, Direction.Down, 100.millis)

  test("an entity (at spawn in this specific map) is blocked by a wall to its left") {
    val moved = entity.move(Direction.Left, isWalkable)
    assert(moved.movement.isEmpty)
    assert(moved.facing == Direction.Down)
  }

  test("an entity (at spawn and in this specific map) can move right, since that tile is floor") {
    val moved = entity.move(Direction.Right, isWalkable)
    assert(moved.movement.contains(Movement(spawn, spawn + Direction.Right, 100.millis)))
    assert(moved.facing == Direction.Right)
  }

  test(
    "an entity (at spawn and in this specific map) can move down, since that tile is a bonus" +
      " (so not a wall)"
  ) {
    val moved = entity.move(Direction.Down, isWalkable)
    assert(moved.movement.contains(Movement(spawn, spawn + Direction.Down, 100.millis)))
    assert(moved.facing == Direction.Down)
  }

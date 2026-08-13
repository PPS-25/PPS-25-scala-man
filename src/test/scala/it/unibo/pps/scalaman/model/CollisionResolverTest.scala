package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.map.io.MapLoader
import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.map.{MapTestSupport, ValidatedMap}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class CollisionResolverTest extends AnyFunSuite, MapTestSupport:

  private val validatedMap: ValidatedMap =
    val validated = MapLoader.load(resourcePath("valid/basic-map.txt")) match
      case Right(text) =>
        MapParser.parse(text) match
          case Right(parsed) => MapValidator.validate(parsed)
          case Left(errors)  => fail(s"$errors")
      case Left(error) => fail(s"$error")
    validated.getOrElse(fail(s"$validated"))

  test("hitting a wall leaves the moving entity unchanged") {
    val entity = MovingEntity(Position(1, 1), Direction.Down, 100.millis)
    assert(CollisionResolver.resolve(entity, Collision.Wall, validatedMap) == entity)
  }

  test(
    "colliding with a teleport moves the entity to the corresponding teleport destination. Teleports work both ways."
  ) {
    val start = Position(1, 3)
    val destination = Position(3, 3)
    val entity = MovingEntity(start, Direction.Down, 100.millis)
    val entityAfterResolvedCollision =
      CollisionResolver.resolve(entity, Collision.Teleport(0), validatedMap)
    assert(entityAfterResolvedCollision.currentPos == destination)
    val resolvedCollisionAtDest =
      CollisionResolver.resolve(entityAfterResolvedCollision, Collision.Teleport(0), validatedMap)
    assert(resolvedCollisionAtDest.currentPos == start)
  }

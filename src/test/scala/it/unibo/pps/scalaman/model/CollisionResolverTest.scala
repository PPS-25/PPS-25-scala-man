package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.map.io.MapLoader
import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.entities.{Enemy, MovingEntity}
import it.unibo.pps.scalaman.model.map.{EnemyKind, MapTestSupport, ValidatedMap}
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

  test("a teleport carries the entity to the other end, and back from there") {
    val start = Position(1, 3)
    val destination = Position(3, 3)
    val entity = MovingEntity(start, Direction.Down, 100.millis)
    val carried = CollisionResolver.teleported(entity, 0, validatedMap)
    assert(carried.currentPos == destination)
    assert(CollisionResolver.teleported(carried, 0, validatedMap).currentPos == start)
  }

  test("a teleport reached by the code of its far end leads to the same pair") {
    val entity = MovingEntity(Position(3, 3), Direction.Down, 100.millis)
    assert(CollisionResolver.teleported(entity, 5, validatedMap).currentPos == Position(1, 3))
  }

  test("an enemy standing on a teleport is carried to the paired end") {
    val enemy = Enemy(MovingEntity(Position(1, 3), Direction.Down, 100.millis), EnemyKind.Hunter)
    assert(
      CollisionResolver.enemyAfterTeleporting(enemy, validatedMap).currentPos == Position(3, 3)
    )
  }

  test("a teleported enemy does not bounce back without leaving the teleport first") {
    val enemy = Enemy(MovingEntity(Position(1, 3), Direction.Down, 100.millis), EnemyKind.Hunter)
    val carried = CollisionResolver.enemyAfterTeleporting(enemy, validatedMap)
    assert(
      CollisionResolver.enemyAfterTeleporting(carried, validatedMap).currentPos == Position(3, 3)
    )
  }

package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.map.io.MapLoader
import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.map.{MapTestSupport, Tile, ValidatedMap}
import org.scalatest.funsuite.AnyFunSuite

class CollisionDetectorTest extends AnyFunSuite, MapTestSupport:

  private val validatedMap: ValidatedMap =
    val validated = MapLoader.load(resourcePath("valid/basic-map.txt")) match
      case Right(text) =>
        MapParser.parse(text) match
          case Right(parsed) => MapValidator.validate(parsed)
          case Left(errors)  => fail(s"$errors")
      case Left(error) => fail(s"$error")
    validated.getOrElse(fail(s"$validated"))

  test("finds collision with wall") {
    assert(
      CollisionDetector.checkForCollision(Position(1, 0), validatedMap, Seq.empty) == Set(
        Collision.Wall
      )
    )
  }

  test("finds no collision on plain floor or spawn points of any kind") {
    assert(
      CollisionDetector.checkForCollision(Position(1, 2), validatedMap, Seq.empty) == Set.empty
    ) // floor
    assert(
      CollisionDetector.checkForCollision(Position(1, 1), validatedMap, Seq.empty) == Set.empty
    ) // Spawn of player
    assert(
      CollisionDetector.checkForCollision(Position(3, 4), validatedMap, Seq.empty) == Set.empty
    ) // Spawn of hunter enemy
    assert(
      CollisionDetector.checkForCollision(Position(3, 5), validatedMap, Seq.empty) == Set.empty
    ) // Spawn of anticipator enemy
  }

  test("finds a teleport start cell") {
    assert(
      CollisionDetector.checkForCollision(Position(1, 3), validatedMap, Seq.empty) == Set(
        Collision.Teleport(0)
      )
    )
  }

  test("finds a teleport destination cell") {
    assert(
      CollisionDetector.checkForCollision(Position(3, 3), validatedMap, Seq.empty) == Set(
        Collision.Teleport(5)
      )
    )
  }

  test("finds an invulnerability bonus") {
    assert(
      CollisionDetector.checkForCollision(Position(1, 4), validatedMap, Seq.empty) == Set(
        Collision.Bonus(Tile.InvulnerabilityBonus)
      )
    )
  }

  test("finds a slowdown bonus") {
    assert(
      CollisionDetector.checkForCollision(Position(2, 1), validatedMap, Seq.empty) == Set(
        Collision.Bonus(Tile.SlowdownBonus)
      )
    )
  }

  test("finds a collectible") {
    assert(
      CollisionDetector.checkForCollision(Position(3, 1), validatedMap, Seq.empty) == Set(
        Collision.Collectible
      )
    )
  }

  test("finds an enemy when one occupies the position") {
    val enemy = Position(3, 4)
    assert(
      CollisionDetector.checkForCollision(Position(3, 4), validatedMap, Seq(enemy)) == Set(
        Collision.Enemy
      )
    )
  }

  test("finds both a tile collision and enemy collision if both are present on that position") {
    val enemyOnCollectible = Position(3, 1)
    assert(
      CollisionDetector.checkForCollision(
        Position(3, 1),
        validatedMap,
        Seq(enemyOnCollectible)
      ) == Set(Collision.Collectible, Collision.Enemy)
    )
  }

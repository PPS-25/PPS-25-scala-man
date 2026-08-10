package it.unibo.pps.scalaman.map.validation

import it.unibo.pps.scalaman.model.map.Cell
import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.map.MapValidationError
import it.unibo.pps.scalaman.model.map.MapTestSupport
import it.unibo.pps.scalaman.model.map.RawMap
import it.unibo.pps.scalaman.map.parser.MapParser
import org.scalatest.funsuite.AnyFunSuite

class MapValidationSpec extends AnyFunSuite, MapTestSupport:
  private val teleportPairs = List(0 -> 5, 1 -> 6, 2 -> 7, 3 -> 8, 4 -> 9)

  private def teleportPairResource(start: Int, paired: Int): String =
    s"valid/teleport-pairs/$start-$paired.txt"

  test("accepts a valid playable map") {
    val parsed = MapParser.parse(resourceText("valid/basic-map.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isRight)
    val map = validated.toOption.get
    assert(map.spawn.row == 1 && map.spawn.col == 1)
    assert(map.collectibles.nonEmpty)
    assert(map.enemies.exists(_.kind == EnemyKind.Hunter))
    assert(map.enemies.exists(_.kind == EnemyKind.Anticipator))
    assert(map.teleports.contains(0))
  }

  test("accepts a valid playable map without teleports") {
    val parsed = MapParser.parse(resourceText("valid/no-teleports.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isRight)
    assert(validated.toOption.get.teleports.isEmpty)
  }

  test("accepts every documented teleport pairing") {
    teleportPairs.foreach { case (start, paired) =>
      val parsed = MapParser.parse(resourceText(teleportPairResource(start, paired))).toOption.get
      val validated = MapValidator.validate(parsed)

      assert(validated.isRight)
      assert(validated.toOption.get.teleports.contains(start))
    }
  }

  test("accepts collectibles reachable only through a teleport") {
    val parsed = MapParser.parse(resourceText("valid/collectible-via-teleport.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isRight)
    assert(validated.toOption.get.collectibles.nonEmpty)
  }

  test("accepts an enemy reachable only through a teleport") {
    val parsed = MapParser.parse(resourceText("valid/enemy-via-teleport.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isRight)
    assert(validated.toOption.get.enemies.nonEmpty)
  }

  test("rejects raw maps with invalid dimensions") {
    val validated = MapValidator.validate(RawMap(Vector.empty))

    assert(validated.isLeft)
    assert(validated.fold(_.contains(MapValidationError.InvalidDimensions(0, 0)), _ => false))
  }

  test("rejects unsupported teleport codes") {
    val validated = MapValidator.validate(
      RawMap(
        Vector(
          Vector(Cell.Wall, Cell.Wall, Cell.Wall),
          Vector(Cell.Wall, Cell.Teleport(12), Cell.Wall),
          Vector(Cell.Wall, Cell.Wall, Cell.Wall)
        )
      )
    )

    assert(validated.isLeft)
    assert(
      validated.fold(
        _.exists {
          case MapValidationError.UnsupportedTeleportCode(12) => true
          case _                                              => false
        },
        _ => false
      )
    )
  }

  test("rejects maps without a spawn") {
    val parsed = MapParser.parse(resourceText("invalid/missing-spawn.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isLeft)
    assert(validated.fold(_.contains(MapValidationError.MissingSpawn), _ => false))
  }

  test("rejects maps with more than one spawn") {
    val parsed = MapParser.parse(resourceText("invalid/spawn-duplicate.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isLeft)
    assert(validated.fold(_.contains(MapValidationError.InvalidSpawnCount(2)), _ => false))
  }

  test("rejects maps without collectibles") {
    val parsed = MapParser.parse(resourceText("invalid/missing-collectible.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isLeft)
    assert(validated.fold(_.contains(MapValidationError.MissingCollectible), _ => false))
  }

  test("rejects maps with a collectible in a separate component") {
    val parsed =
      MapParser.parse(resourceText("invalid/collectible-separate-component.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isLeft)
    assert(
      validated.fold(
        _.exists {
          case MapValidationError.UnreachableCollectible(_) => true
          case _                                            => false
        },
        _ => false
      )
    )
  }

  test("rejects maps without enemies") {
    val parsed = MapParser.parse(resourceText("invalid/missing-enemy.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isLeft)
    assert(validated.fold(_.contains(MapValidationError.MissingEnemy), _ => false))
  }

  test("rejects maps with an enemy in a separate component") {
    val parsed = MapParser.parse(resourceText("invalid/enemy-separate-component.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isLeft)
    assert(
      validated.fold(
        _.exists {
          case MapValidationError.UnreachableEnemy(_) => true
          case _                                      => false
        },
        _ => false
      )
    )
  }

  test("rejects unreachable collectibles") {
    val parsed = MapParser.parse(resourceText("invalid/unreachable-collectible.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isLeft)
    assert(
      validated.fold(
        _.exists {
          case MapValidationError.UnreachableCollectible(_) => true
          case _                                            => false
        },
        _ => false
      )
    )
  }

  test("rejects incomplete teleport pairs") {
    val parsed = MapParser.parse(resourceText("invalid/incomplete-teleport.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isLeft)
    assert(
      validated.fold(
        _.exists {
          case MapValidationError.InvalidTeleportPair(0, 1) => true
          case _                                            => false
        },
        _ => false
      )
    )
  }

  test("rejects duplicated teleport cells") {
    val parsed = MapParser.parse(resourceText("invalid/duplicate-teleport.txt")).toOption.get
    val validated = MapValidator.validate(parsed)

    assert(validated.isLeft)
    assert(
      validated.fold(
        _.exists {
          case MapValidationError.InvalidTeleportPair(0, 3) => true
          case _                                            => false
        },
        _ => false
      )
    )
  }
end MapValidationSpec

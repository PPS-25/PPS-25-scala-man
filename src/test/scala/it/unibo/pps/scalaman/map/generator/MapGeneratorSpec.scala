package it.unibo.pps.scalaman.map.generator

import it.unibo.pps.scalaman.model.map.Cell
import it.unibo.pps.scalaman.model.map.MapGenerationError
import it.unibo.pps.scalaman.model.map.MapGenerationSpec
import it.unibo.pps.scalaman.model.map.MapTestSupport
import it.unibo.pps.scalaman.map.validation.MapValidator
import org.scalatest.funsuite.AnyFunSuite

class MapGeneratorSpec extends AnyFunSuite, MapTestSupport:
  private def countCells(map: it.unibo.pps.scalaman.model.map.RawMap)(
      predicate: Cell => Boolean
  ): Int =
    map.rows.flatten.count(predicate)

  private def countEnemies(map: it.unibo.pps.scalaman.model.map.RawMap): Int =
    countCells(map) {
      case Cell.Hunter | Cell.Anticipator => true
      case _                              => false
    }

  test("generates a map that passes validation") {
    val generated = MapGenerator.generate(
      MapGenerationSpec(
        width = 7,
        height = 5,
        collectibles = 1,
        teleports = 1,
        enemies = 1,
        seed = Some(42L)
      )
    )

    assert(generated.isRight)
    val parsed = generated.toOption.get
    assert(parsed.width == 7)
    assert(parsed.height == 5)
    assert(MapValidator.validate(parsed).isRight)
  }

  test("generates exact entity counts and paired teleports") {
    val generated = MapGenerator.generate(
      MapGenerationSpec(
        width = 8,
        height = 6,
        collectibles = 2,
        teleports = 2,
        enemies = 2,
        seed = Some(99L)
      )
    )

    assert(generated.isRight)
    val map = generated.toOption.get

    assert(map.width == 8)
    assert(map.height == 6)
    assert(countCells(map)(_ == Cell.Spawn) == 1)
    assert(countCells(map)(_ == Cell.Collectible) == 2)
    assert(countEnemies(map) == 2)
    assert(countCells(map) {
      case Cell.Teleport(code) if code >= 0 && code <= 9 => true
      case _                                             => false
    } == 4)
    assert(MapValidator.validate(map).isRight)
  }

  test("is deterministic with the same seed") {
    val spec = MapGenerationSpec(
      width = 7,
      height = 5,
      collectibles = 1,
      teleports = 1,
      enemies = 1,
      seed = Some(123L)
    )

    val first = MapGenerator.generate(spec)
    val second = MapGenerator.generate(spec)

    assert(first == second)
  }

  test("rejects invalid generation specifications") {
    val invalidSpecs = List(
      MapGenerationSpec(width = 0, height = 5, collectibles = 1, teleports = 1, enemies = 1),
      MapGenerationSpec(width = 5, height = 0, collectibles = 1, teleports = 1, enemies = 1),
      MapGenerationSpec(width = 5, height = 5, collectibles = -1, teleports = 1, enemies = 1),
      MapGenerationSpec(width = 5, height = 5, collectibles = 0, teleports = 1, enemies = 1),
      MapGenerationSpec(width = 5, height = 5, collectibles = 1, teleports = -1, enemies = 1),
      MapGenerationSpec(width = 5, height = 5, collectibles = 1, teleports = 1, enemies = 0),
      MapGenerationSpec(width = 3, height = 3, collectibles = 1, teleports = 1, enemies = 1),
      MapGenerationSpec(width = 7, height = 5, collectibles = 1, teleports = 6, enemies = 1)
    )

    invalidSpecs.foreach { spec =>
      val generated = MapGenerator.generate(spec)

      assert(generated.isLeft)
      assert(
        generated.fold(
          _.exists {
            case MapGenerationError.InvalidSpecification(_) => true
            case _                                          => false
          },
          _ => false
        )
      )
    }
  }
end MapGeneratorSpec

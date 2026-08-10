package it.unibo.pps.scalaman.map.parser

import it.unibo.pps.scalaman.model.map.Cell
import it.unibo.pps.scalaman.model.map.MapParseError
import it.unibo.pps.scalaman.model.map.MapTestSupport
import it.unibo.pps.scalaman.model.map.RawMap
import org.scalatest.funsuite.AnyFunSuite

class MapParserSpec extends AnyFunSuite, MapTestSupport:
  private val teleportPairs = List(0 -> 5, 1 -> 6, 2 -> 7, 3 -> 8, 4 -> 9)

  private def assertCell(map: RawMap, row: Int, col: Int, expected: Cell): Unit =
    assert(map.rows(row)(col) == expected)

  test(
    "parses a valid rectangular map with spawn, collectible, enemies, bonuses, and a teleport pair"
  ) {
    val parsed = MapParser.parse(resourceText("valid/basic-map.txt"))

    assert(parsed.isRight)
    val map = parsed.toOption.get
    assert(map.height == 5)
    assert(map.width == 7)
    assert(map.rows.flatten.contains(Cell.Spawn))
    assert(map.rows.flatten.contains(Cell.Collectible))
    assert(map.rows.flatten.contains(Cell.Hunter))
    assert(map.rows.flatten.contains(Cell.Anticipator))
    assert(map.rows.flatten.contains(Cell.InvulnerabilityBonus))
    assert(map.rows.flatten.contains(Cell.SlowdownBonus))
    assert(map.rows.flatten.exists {
      case Cell.Teleport(0) => true
      case Cell.Teleport(5) => true
      case _                => false
    })
  }

  test("maps every documented base and overlay symbol to the expected cell") {
    val parsed = MapParser.parse(resourceText("valid/symbol-mapping.txt"))

    assert(parsed.isRight)
    val map = parsed.toOption.get
    assertCell(map, 1, 1, Cell.Floor)
    assertCell(map, 1, 2, Cell.Spawn)
    assertCell(map, 1, 3, Cell.Collectible)
    assertCell(map, 1, 4, Cell.Hunter)
    assertCell(map, 1, 5, Cell.Anticipator)
    assertCell(map, 1, 6, Cell.InvulnerabilityBonus)
    assertCell(map, 1, 7, Cell.SlowdownBonus)
    assertCell(map, 1, 8, Cell.Floor)
  }

  test("maps every teleport digit to the matching teleport cell") {
    val parsed = MapParser.parse(resourceText("valid/teleport-digits.txt"))

    assert(parsed.isRight)
    val map = parsed.toOption.get
    (0 to 9).foreach { code =>
      assertCell(map, 1, code + 1, Cell.Teleport(code))
    }
  }

  test("parses every documented teleport pairing") {
    teleportPairs.foreach { case (start, paired) =>
      val parsed = MapParser.parse(resourceText(s"valid/teleport-pairs/$start-$paired.txt"))

      assert(parsed.isRight)
      val map = parsed.toOption.get
      assert(map.rows.flatten.exists(_ == Cell.Teleport(start)))
      assert(map.rows.flatten.exists(_ == Cell.Teleport(paired)))
    }
  }

  test("rejects empty maps") {
    val parsed = MapParser.parse(resourceText("invalid/empty-map.txt"))

    assert(parsed == Left(List(MapParseError.EmptyMap)))
  }

  test("rejects malformed maps with ragged rows") {
    val parsed = MapParser.parse(resourceText("invalid/ragged-map.txt"))

    assert(parsed.isLeft)
    assert(
      parsed.fold(
        _.exists {
          case MapParseError.RaggedRow(_, _, _) => true
          case _                                => false
        },
        _ => false
      )
    )
  }

  test("rejects unsupported symbols") {
    val parsed = MapParser.parse(resourceText("invalid/unsupported-symbol.txt"))

    assert(parsed.isLeft)
    assert(
      parsed.fold(
        _.exists {
          case MapParseError.UnsupportedSymbol(_, _, _) => true
          case _                                        => false
        },
        _ => false
      )
    )
  }
end MapParserSpec

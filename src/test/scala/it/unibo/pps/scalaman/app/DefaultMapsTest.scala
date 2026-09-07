package it.unibo.pps.scalaman.app

import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import org.scalatest.funsuite.AnyFunSuite

class DefaultMapsTest extends AnyFunSuite:

  private def refused(map: MapName): Seq[String] =
    DefaultMaps
      .textOf(map)
      .toSeq
      .flatMap(text =>
        MapParser.parse(text).flatMap(MapValidator.validate).left.toSeq.flatten.map(_.toString)
      )

  test("every maze the game ships with is there to be read") {
    assert(DefaultMaps.All.filter(DefaultMaps.textOf(_).isEmpty) == Seq.empty)
  }

  test("every maze the game ships with is one the game accepts") {
    assert(DefaultMaps.All.flatMap(refused) == Seq.empty)
  }

package it.unibo.pps.scalaman.map

import it.unibo.pps.scalaman.model.map.MapTestSupport
import it.unibo.pps.scalaman.map.io.MapLoader
import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import org.scalatest.funsuite.AnyFunSuite

class MapPipelineSpec extends AnyFunSuite, MapTestSupport:
  test("loads, parses, and validates a valid map file end to end") {
    val validated = MapLoader.load(resourcePath("valid/basic-map.txt")) match
      case Right(text) =>
        MapParser.parse(text) match
          case Right(parsed) => MapValidator.validate(parsed)
          case Left(errors)  => fail(s"Parsing failed unexpectedly: $errors")
      case Left(error) => fail(s"Loading failed unexpectedly: $error")

    assert(validated.isRight)
  }

  test("rejects an invalid map file end to end") {
    val validated = MapLoader.load(resourcePath("invalid/missing-spawn.txt")) match
      case Right(text) =>
        MapParser.parse(text) match
          case Right(parsed) => MapValidator.validate(parsed)
          case Left(errors)  => fail(s"Parsing failed unexpectedly: $errors")
      case Left(error) => fail(s"Loading failed unexpectedly: $error")

    assert(validated.isLeft)
  }
end MapPipelineSpec

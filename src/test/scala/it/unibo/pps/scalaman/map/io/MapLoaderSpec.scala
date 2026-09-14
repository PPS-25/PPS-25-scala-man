package it.unibo.pps.scalaman.map.io

import java.nio.file.Files
import java.nio.file.Paths

import it.unibo.pps.scalaman.model.map.MapLoadError
import it.unibo.pps.scalaman.model.map.MapTestSupport
import org.scalatest.funsuite.AnyFunSuite

class MapLoaderSpec extends AnyFunSuite, MapTestSupport:
  test("loads an existing map file as raw text") {
    val expected = resourceText("valid/basic-map.txt")
    val loaded = MapLoader.load(resourcePath("valid/basic-map.txt"))

    assert(loaded == Right(expected))
  }

  test("fails when the map file does not exist") {
    val missing = Paths.get("src/test/resources/maps/invalid/missing-file.txt")
    val loaded = MapLoader.load(missing)

    assert(loaded.isLeft)
    assert(loaded.left.exists {
      case MapLoadError.FileNotFound(_) => true
      case _                            => false
    })
  }

  test("loads an empty file as empty raw text") {
    val emptyFile = Files.createTempFile("map-loader-empty", ".txt")
    try {
      val loaded = MapLoader.load(emptyFile)

      assert(loaded == Right(""))
    } finally Files.deleteIfExists(emptyFile)
  }

  test("fails when the path is not a readable file") {
    val unreadable = Files.createTempDirectory("map-loader-unreadable")
    try {
      val loaded = MapLoader.load(unreadable)

      assert(loaded.isLeft)
      assert(loaded.left.exists {
        case MapLoadError.ReadFailed(_, _) => true
        case _                             => false
      })
    } finally Files.deleteIfExists(unreadable)
  }
end MapLoaderSpec

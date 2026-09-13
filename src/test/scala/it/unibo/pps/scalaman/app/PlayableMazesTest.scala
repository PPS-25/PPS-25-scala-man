package it.unibo.pps.scalaman.app

import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Path, Paths}

class PlayableMazesTest extends AnyFunSuite:

  private val folder = Paths.get("/somewhere/maps")
  private def found(names: String*): Seq[Path] = names.map(folder.resolve)

  test("a maze file is known by its name, without the extension") {
    assert(PlayableMazes.named(folder.resolve("spirale.txt")).contains(MapName("spirale")))
  }

  test("a maze file keeps every dot of its name but the last") {
    assert(
      PlayableMazes.named(folder.resolve("quello.di.alex.txt")).contains(MapName("quello.di.alex"))
    )
  }

  test("a file with nothing left once the extension is gone names no maze") {
    assert(PlayableMazes.named(folder.resolve(".txt")).isEmpty)
  }

  test("the mazes the game ships with are always offered") {
    assert(PlayableMazes.offered(Seq.empty) == DefaultMaps.All)
  }

  test("a maze of whoever plays is offered after the ones the game ships with") {
    assert(PlayableMazes.offered(found("spirale.txt")) == DefaultMaps.All :+ MapName("spirale"))
  }

  test("a file that is not a maze is not offered") {
    assert(PlayableMazes.offered(found("appunti.md")) == DefaultMaps.All)
  }

  test("a maze of whoever plays named after a shipped one is left out") {
    assert(PlayableMazes.offered(found("medium.txt")) == DefaultMaps.All)
  }

  test("a maze is offered once, however many files are found for it") {
    assert(
      PlayableMazes.offered(found("spirale.txt", "spirale.txt")) == DefaultMaps.All :+ MapName(
        "spirale"
      )
    )
  }

  test("a maze the game ships with is read from inside the application, not from a file") {
    assert(PlayableMazes.fileOf(MapName("medium"), folder).isEmpty)
  }

  test("a maze of whoever plays is read from their own folder") {
    assert(PlayableMazes.fileOf(MapName("spirale"), folder).contains(folder.resolve("spirale.txt")))
  }

package it.unibo.pps.scalaman.app

import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Paths

class GameFilesTest extends AnyFunSuite:

  private val files = GameFiles(Paths.get("/somewhere"))

  test("the saved games are kept together, under the home of the game") {
    assert(files.saves.startsWith(files.home))
  }

  test("a maze has a leaderboard of its own, told apart by its name") {
    assert(files.leaderboardOf(MapName("arena")) != files.leaderboardOf(MapName("classic")))
  }

  test("a leaderboard is named after the maze it belongs to") {
    assert(files.leaderboardOf(MapName("arena")).getFileName.toString == "arena.csv")
  }

  test("a maze cannot go without a name") {
    assertThrows[IllegalArgumentException](MapName(""))
  }

  test("a player cannot go without a name") {
    assertThrows[IllegalArgumentException](PlayerName("  "))
  }

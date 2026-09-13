package it.unibo.pps.scalaman.app

import org.scalatest.funsuite.AnyFunSuite
import it.unibo.pps.scalaman.model.LeaderboardMode

import java.nio.file.Paths

class GameFilesTest extends AnyFunSuite:

  private val files = GameFiles(Paths.get("/somewhere"))

  test("the saved games are kept together, under the home of the game") {
    assert(files.saves.startsWith(files.home))
  }

  test("the mazes whoever plays added are kept together, under the home of the game") {
    assert(files.mazes.startsWith(files.home))
  }

  test("the player name is kept under the home of the game") {
    assert(files.playerName == files.home.resolve("player-name.txt"))
  }

  test("a maze has a leaderboard of its own, told apart by its name") {
    assert(files.leaderboardOf(MapName("arena")) != files.leaderboardOf(MapName("classic")))
  }

  test("a leaderboard is named after the maze it belongs to") {
    assert(files.leaderboardOf(MapName("arena")).getFileName.toString == "arena.csv")
  }

  test("leaderboards for different modes of a maze have different files") {
    val arena = MapName("arena")
    assert(
      files.leaderboardOf(arena, LeaderboardMode.Classic) !=
        files.leaderboardOf(arena, LeaderboardMode.Timed)
    )
  }

  test("a maze cannot go without a name") {
    assertThrows[IllegalArgumentException](MapName(""))
  }

  test("a maze name cannot escape the game data folders") {
    assertThrows[IllegalArgumentException](MapName("../../outside"))
    assertThrows[IllegalArgumentException](MapName("maps\\outside"))
  }

  test("a player cannot go without a name") {
    assertThrows[IllegalArgumentException](PlayerName("  "))
  }

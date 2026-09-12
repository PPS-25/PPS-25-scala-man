package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.app.{DefaultMaps, GameFiles, MapName, Played, PlayerName}
import it.unibo.pps.scalaman.model.{LeaderboardMode, LevelState, LevelTestSupport}
import it.unibo.pps.scalaman.model.score.GameResult
import it.unibo.pps.scalaman.persistence.{PropertiesGameSaveRepository, SavedGame}
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path}
import java.time.{Instant, LocalDateTime}
import scala.jdk.CollectionConverters.*

class GameFilesEnvironmentTest extends AnyFunSuite:

  private val player = PlayerName("Matilde D'Antino")
  private val medium = MapName("medium")
  private val mine = MapName("spirale")
  private val anyGame = LevelState.from(LevelTestSupport.maze)

  private def inItsOwnHome(check: (GameFilesEnvironment, GameFiles) => Unit): Unit =
    inItsOwnHomeAt(() => LocalDateTime.now())(check)

  private def inItsOwnHomeAt(now: () => LocalDateTime)(
      check: (GameFilesEnvironment, GameFiles) => Unit
  ): Unit =
    val home = Files.createTempDirectory("scalaManTest")
    try
      val files = GameFiles(home)
      check(GameFilesEnvironment(files, PropertiesGameSaveRepository(), now), files)
    finally deleting(home)

  private def deleting(path: Path): Unit =
    if Files.isDirectory(path) then
      val within = Files.list(path)
      try within.iterator.asScala.toSeq.foreach(deleting)
      finally within.close()
    Files.deleteIfExists(path)

  private def maze(named: MapName, in: Path): Path =
    Files.createDirectories(in)
    Files.writeString(in.resolve(s"${named.value}.txt"), DefaultMaps.textOf(medium).get)

  private def onlySavedFile(in: Path): Path =
    val saved = Files.list(in)
    try
      val files = saved.iterator.asScala.toSeq
      assert(files.size == 1)
      files.head
    finally saved.close()

  test("the mazes the game ships with are offered even before anything was added") {
    inItsOwnHome((world, _) => assert(world.mazes == DefaultMaps.All))
  }

  test("a maze added to the files of whoever plays is offered as well") {
    inItsOwnHome { (world, files) =>
      maze(mine, files.mazes)
      assert(world.mazes.contains(mine))
    }
  }

  test("a maze the game ships with is read without looking for a file") {
    inItsOwnHome((world, _) => assert(world.maze(medium).isRight))
  }

  test("a maze added to the files of whoever plays is read from there") {
    inItsOwnHome { (world, files) =>
      maze(mine, files.mazes)
      assert(world.maze(mine).isRight)
    }
  }

  test("a missing maze tells where its file was expected") {
    inItsOwnHome { (world, files) =>
      val missing = world.mazeAt(files.home.resolve("nowhere.txt"))
      assert(missing == Left(s"there is no file at ${files.home.resolve("nowhere.txt")}"))
    }
  }

  test("an invalid maze tells why it cannot be played") {
    inItsOwnHome { (world, files) =>
      val open = Files.writeString(files.home.resolve("open.txt"), "S.C\n...\n..H")
      val refused = world.mazeAt(open).swap.getOrElse("")
      assert(refused == "the maze is open at 8 places along its border")
    }
  }

  test("a maze read from elsewhere is kept among the files of whoever plays") {
    inItsOwnHome { (world, files) =>
      val elsewhere =
        Files.writeString(files.home.resolve("spirale.txt"), DefaultMaps.textOf(medium).get)
      world.keeping(elsewhere)
      assert(Files.exists(files.mazes.resolve("spirale.txt")))
    }
  }

  test("a maze already kept is not written over") {
    inItsOwnHome { (world, files) =>
      val kept = Files.writeString(maze(mine, files.mazes), "the one that was there first")
      world.keeping(maze(mine, files.home))
      assert(Files.readString(kept) == "the one that was there first")
    }
  }

  test("a maze already kept is nothing to complain about") {
    inItsOwnHome { (world, files) =>
      maze(mine, files.mazes)
      assert(world.keeping(maze(mine, files.home)) == Right(()))
    }
  }

  test("a maze named after one the game ships with is not kept") {
    inItsOwnHome { (world, files) =>
      val elsewhere =
        Files.writeString(files.home.resolve("medium.txt"), DefaultMaps.textOf(medium).get)
      world.keeping(elsewhere)
      assert(!Files.exists(files.mazes.resolve("medium.txt")))
    }
  }

  test("a game put away is named after its maze, player, and local save time") {
    inItsOwnHome { (world, files) =>
      world.saving(anyGame, Played(player, Some(medium)))
      assert(
        onlySavedFile(files.saves).getFileName.toString.matches(
          "medium-Matilde-D-Antino-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}-\\d{3}\\.properties"
        )
      )
    }
  }

  test("a game put away is read back as the game it was") {
    inItsOwnHome { (world, files) =>
      world.saving(anyGame, Played(player, Some(medium)))
      val read = world.savedGame(onlySavedFile(files.saves))
      assert(read == Right(SavedGame(anyGame, Some(medium))))
    }
  }

  test("games saved at the same instant keep separate files") {
    val instant = LocalDateTime.of(2026, 1, 2, 3, 4, 5, 6_000_000)
    inItsOwnHomeAt(() => instant) { (world, files) =>
      world.saving(anyGame, Played(player, Some(medium)))
      world.saving(anyGame, Played(player, Some(medium)))
      val saved = Files.list(files.saves)
      try
        assert(
          saved.iterator.asScala.map(_.getFileName.toString).toSet == Set(
            "medium-Matilde-D-Antino-2026-01-02_03-04-05-006.properties",
            "medium-Matilde-D-Antino-2026-01-02_03-04-05-006-1.properties"
          )
        )
      finally saved.close()
    }
  }

  test("a game that was never put away cannot be read back") {
    inItsOwnHome((world, files) => assert(world.savedGame(files.saves.resolve("none")).isLeft))
  }

  test("a remembered player name is offered again") {
    inItsOwnHome { (world, _) =>
      assert(world.remembering(player) == Right(()))
      assert(world.playerName.contains(player))
    }
  }

  test("remembering the same player name does not rewrite it") {
    inItsOwnHome { (world, files) =>
      world.remembering(player)
      val firstWritten = FileTime.fromMillis(1)
      Files.setLastModifiedTime(files.playerName, firstWritten)

      assert(world.remembering(player) == Right(()))
      assert(Files.getLastModifiedTime(files.playerName) == firstWritten)
    }
  }

  test("a changed player name replaces the remembered one") {
    inItsOwnHome { (world, _) =>
      val changed = PlayerName("Gaia")
      world.remembering(player)
      assert(world.remembering(changed) == Right(()))
      assert(world.playerName.contains(changed))
    }
  }

  test("a maze nobody played in a mode has no best scores") {
    inItsOwnHome((world, _) =>
      assert(world.bestOn(medium, LeaderboardMode.Classic).entries.isEmpty)
    )
  }

  test("a score recorded on a maze is among the best scores of that map and mode") {
    inItsOwnHome { (world, _) =>
      val result = GameResult(player.value, 100, Instant.parse("2026-01-01T00:00:00Z"))
      world.recording(result, medium, LeaderboardMode.Timed)
      assert(world.bestOn(medium, LeaderboardMode.Timed).entries == List(result))
    }
  }

  test("scores for different modes on a maze are kept apart") {
    inItsOwnHome { (world, _) =>
      world.recording(
        GameResult(player.value, 100, Instant.parse("2026-01-01T00:00:00Z")),
        medium,
        LeaderboardMode.Classic
      )
      assert(world.bestOn(medium, LeaderboardMode.Survival).entries.isEmpty)
    }
  }

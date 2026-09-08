package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.app.{DefaultMaps, GameFiles, MapName, Played, PlayerName}
import it.unibo.pps.scalaman.model.{LeaderboardMode, LevelState, LevelTestSupport}
import it.unibo.pps.scalaman.model.score.GameResult
import it.unibo.pps.scalaman.persistence.PropertiesGameSaveRepository
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path}
import java.time.Instant
import scala.jdk.CollectionConverters.*

class GameFilesEnvironmentTest extends AnyFunSuite:

  private val player = PlayerName("Matilde D'Antino")
  private val arena = MapName("arena")
  private val mine = MapName("spirale")
  private val anyGame = LevelState.from(LevelTestSupport.maze)

  /** A world of its own for one test, thrown away afterwards. */
  private def inItsOwnHome(check: (GameFilesEnvironment, GameFiles) => Unit): Unit =
    val home = Files.createTempDirectory("scalaManTest")
    try
      val files = GameFiles(home)
      check(GameFilesEnvironment(files, PropertiesGameSaveRepository()), files)
    finally deleting(home)

  private def deleting(path: Path): Unit =
    if Files.isDirectory(path) then
      val within = Files.list(path)
      try within.iterator.asScala.toSeq.foreach(deleting)
      finally within.close()
    Files.deleteIfExists(path)

  /** A maze written under a name, in the folder it is asked for. */
  private def maze(named: MapName, in: Path): Path =
    Files.createDirectories(in)
    Files.writeString(in.resolve(s"${named.value}.txt"), DefaultMaps.textOf(arena).get)

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
    inItsOwnHome((world, _) => assert(world.maze(arena).isRight))
  }

  test("a maze added to the files of whoever plays is read from there") {
    inItsOwnHome { (world, files) =>
      maze(mine, files.mazes)
      assert(world.maze(mine).isRight)
    }
  }

  test("a file that is not there is told apart from a file that is not a maze") {
    inItsOwnHome { (world, files) =>
      val missing = world.mazeAt(files.home.resolve("nowhere.txt"))
      val nonsense = Files.writeString(files.home.resolve("nonsense.txt"), "not a maze at all")
      assert(missing != world.mazeAt(nonsense))
    }
  }

  test("a maze the game refuses is told in words, and not in the shape of an error") {
    inItsOwnHome { (world, files) =>
      val open = Files.writeString(files.home.resolve("open.txt"), "S.C\n...\n..H")
      val refused = world.mazeAt(open).swap.getOrElse("")
      // Brackets and the word Error are the shape a case class prints in, not the shape of a
      // sentence: whoever reads this is playing a game.
      assert(refused.nonEmpty && !refused.contains("(") && !refused.contains("Error"))
    }
  }

  test("a maze read from elsewhere is kept among the files of whoever plays") {
    inItsOwnHome { (world, files) =>
      val elsewhere =
        Files.writeString(files.home.resolve("spirale.txt"), DefaultMaps.textOf(arena).get)
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

  // Keeping is asked for on every maze read from elsewhere, so a maze already kept must be a quiet
  // no-op: reported as a failure, it would put an error in front of whoever just chose it.
  test("a maze already kept is nothing to complain about") {
    inItsOwnHome { (world, files) =>
      maze(mine, files.mazes)
      assert(world.keeping(maze(mine, files.home)) == Right(()))
    }
  }

  test("a maze named after one the game ships with is not kept") {
    inItsOwnHome { (world, files) =>
      val elsewhere =
        Files.writeString(files.home.resolve("arena.txt"), DefaultMaps.textOf(arena).get)
      world.keeping(elsewhere)
      assert(!Files.exists(files.mazes.resolve("arena.txt")))
    }
  }

  test("a game put away is named after the maze and whoever played it") {
    inItsOwnHome { (world, files) =>
      world.saving(anyGame, Played(player, Some(arena)))
      assert(Files.exists(files.saves.resolve("arena-Matilde-D-Antino.properties")))
    }
  }

  test("a game put away is read back as the game it was") {
    inItsOwnHome { (world, files) =>
      world.saving(anyGame, Played(player, Some(arena)))
      val read = world.savedGame(files.saves.resolve("arena-Matilde-D-Antino.properties"))
      assert(read == Right(anyGame))
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
    inItsOwnHome((world, _) => assert(world.bestOn(arena, LeaderboardMode.Classic).entries.isEmpty))
  }

  test("a score recorded on a maze is among the best scores of that map and mode") {
    inItsOwnHome { (world, _) =>
      val result = GameResult(player.value, 100, Instant.parse("2026-01-01T00:00:00Z"))
      world.recording(result, arena, LeaderboardMode.Timed)
      assert(world.bestOn(arena, LeaderboardMode.Timed).entries == List(result))
    }
  }

  test("scores for different modes on a maze are kept apart") {
    inItsOwnHome { (world, _) =>
      world.recording(GameResult(player.value, 100, Instant.now()), arena, LeaderboardMode.Classic)
      assert(world.bestOn(arena, LeaderboardMode.Survival).entries.isEmpty)
    }
  }

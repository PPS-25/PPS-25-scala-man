package it.unibo.pps.scalaman.leaderboard.io

import it.unibo.pps.scalaman.model.score.{Leaderboard, LeaderboardError}
import it.unibo.pps.scalaman.model.score.LeaderboardTestSupport.{board, result}
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Files

class FileLeaderboardStorageTest extends AnyFunSuite:

  test("loading a file that does not exist gives an empty leaderboard") {
    val dir = Files.createTempDirectory("leaderboardTest")
    try
      val storage = FileLeaderboardStorage(dir.resolve("leaderboard.csv"))
      assert(storage.load() == Right(Leaderboard.empty))
    finally Files.deleteIfExists(dir)
  }

  test("loading an empty file gives an empty leaderboard") {
    val path = Files.createTempFile("leaderboardEmpty", ".csv")
    try
      val storage = FileLeaderboardStorage(path)
      assert(storage.load() == Right(Leaderboard.empty))
    finally Files.deleteIfExists(path)
  }

  test("loading a directory fails as a read error") {
    val directory = Files.createTempDirectory("leaderboard-directory")
    try
      FileLeaderboardStorage(directory).load() match
        case Left(_: LeaderboardError.ReadFailed) => succeed
        case other                                => fail(s"expected a read error, got $other")
    finally Files.deleteIfExists(directory)
  }

  test("saving then loading gives back the same leaderboard") {
    val dir = Files.createTempDirectory("leaderboardTest")
    val path = dir.resolve("leaderboard.csv")
    try
      val storage = FileLeaderboardStorage(path)
      val original = board(result("a", 100), result("b", 100))
      assert(storage.save(original) == Right(()))
      assert(storage.load() == Right(original))
    finally {
      Files.deleteIfExists(path)
      Files.deleteIfExists(dir)
    }
  }

  test("loading a malformed file fails with Malformed") {
    val path = Files.createTempFile("leaderboardMalformed", ".csv")
    try
      Files.writeString(path, "invalid csv")
      FileLeaderboardStorage(path).load() match
        case Left(_: LeaderboardError.Malformed) => succeed
        case other                               => fail(s"expected Malformed")
    finally Files.deleteIfExists(path)
  }

  test("saving creates a parent directory if it does not exist yet") {
    val dir = Files.createTempDirectory("leaderboardTest")
    val nested = dir.resolve("nested").resolve("leaderboard.csv")
    try
      val storage = FileLeaderboardStorage(nested)
      assert(storage.save(Leaderboard.empty) == Right(()))
      assert(Files.exists(nested))
    finally
      Files.deleteIfExists(nested)
      Files.deleteIfExists(nested.getParent)
      Files.deleteIfExists(dir)
  }

  test("saving to a directory fails as a write error") {
    val directory = Files.createTempDirectory("leaderboard-directory")
    try
      FileLeaderboardStorage(directory).save(Leaderboard.empty) match
        case Left(_: LeaderboardError.WriteFailed) => succeed
        case other                                 => fail(s"expected a write error, got $other")
    finally Files.deleteIfExists(directory)
  }

package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.leaderboard.io.FileLeaderboardStorage
import it.unibo.pps.scalaman.model.score.{GameResult, Leaderboard, LeaderboardStorage}
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Files
import java.time.Instant

class LeaderboardRecordingTest extends AnyFunSuite:

  private val achievedAt = Instant.parse("2026-01-01T00:00:00Z")
  private val res = GameResult("A", 100, achievedAt)

  test("no result leaves the storage untouched") {
    val dir = Files.createTempDirectory("leaderboardTest")
    val path = dir.resolve("leaderboard.csv")
    try
      val storage = FileLeaderboardStorage(path)
      LeaderboardRecording[Int](_ => None, storage).recording(0)
      assert(!Files.exists(path))
    finally Files.deleteIfExists(dir)
  }

  test("a result gets recorded and saved") {
    val dir = Files.createTempDirectory("leaderboardTest")
    val path = dir.resolve("leaderboard.csv")
    try
      val storage = FileLeaderboardStorage(path)
      val result = GameResult("A", 100, achievedAt)
      LeaderboardRecording[Int](_ => Some(result), storage).recording(0)
      assert(storage.load().map(_.top(1)) == Right(List(result)))
    finally
      Files.deleteIfExists(path)
      Files.deleteIfExists(dir)

  }

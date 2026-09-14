package it.unibo.pps.scalaman.model.score

import java.time.Instant

object LeaderboardTestSupport:

  val Reference: Instant = Instant.parse("2026-01-01T00:00:00Z")

  def result(name: String, score: Int, at: Long = 0): GameResult =
    GameResult(name, score, Reference.plusSeconds(at))

  def board(results: GameResult*): Leaderboard = Leaderboard.of(results.toList)

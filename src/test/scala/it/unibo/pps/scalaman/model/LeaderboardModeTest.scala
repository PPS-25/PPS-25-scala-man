package it.unibo.pps.scalaman.model

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class LeaderboardModeTest extends AnyFunSuite:

  test("each game mode belongs to its matching leaderboard category") {
    assert(LeaderboardMode.of(GameMode.Normal) == LeaderboardMode.Classic)
    assert(LeaderboardMode.of(GameMode.Timed(1.second)) == LeaderboardMode.Timed)
    assert(LeaderboardMode.of(GameMode.Survival()) == LeaderboardMode.Survival)
  }

  test("each menu choice selects its matching leaderboard category") {
    assert(LeaderboardMode.of(ModeChoice.Normal) == LeaderboardMode.Classic)
    assert(LeaderboardMode.of(ModeChoice.Timed) == LeaderboardMode.Timed)
    assert(LeaderboardMode.of(ModeChoice.Survival) == LeaderboardMode.Survival)
  }

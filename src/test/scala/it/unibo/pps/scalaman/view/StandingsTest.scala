package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.score.{GameResult, Leaderboard}
import org.scalatest.funsuite.AnyFunSuite

import java.time.{Instant, ZoneId}

class StandingsTest extends AnyFunSuite:

  private val whenever = Instant.parse("2026-08-30T21:45:00Z")

  private def played(name: String, score: Int) = GameResult(name, score, whenever)

  test("a maze nobody has played has no places to show") {
    assert(Standings.of(Leaderboard.empty).isEmpty)
  }

  test("a place is told with who reached it, the score and when") {
    val told = Standings.of(Leaderboard.of(List(played("Gaia", 900))))
    assert(told == Seq(Standing(1, "Gaia", 900, whenever)))
  }

  test("the best score is told first") {
    val board = Leaderboard.of(List(played("Alex", 300), played("Matilde", 1200)))
    assert(Standings.of(board).head.player == "Matilde")
  }

  test("every place the leaderboard holds is told") {
    val many = List.tabulate(20)(n => played(s"player$n", n))
    assert(Standings.of(Leaderboard.of(many)).size == 20)
  }

  test("the places are numbered from the first down") {
    val many = List.tabulate(3)(n => played(s"player$n", n))
    assert(Standings.of(Leaderboard.of(many)).map(_.place) == Seq(1, 2, 3))
  }

  test("when a game was played is told where whoever reads it lives") {
    assert(Standings.dated(whenever, ZoneId.of("Europe/Rome")) == "30/08/2026 23:45")
  }

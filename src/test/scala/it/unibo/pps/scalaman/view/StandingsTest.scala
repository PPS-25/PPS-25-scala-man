package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.score.{GameResult, Leaderboard}
import org.scalatest.funsuite.AnyFunSuite

import java.time.Instant

class StandingsTest extends AnyFunSuite:

  private def played(name: String, score: Int) = GameResult(name, score, Instant.EPOCH)

  test("a maze nobody has played says so") {
    assert(Standings.told(Leaderboard.empty) == Seq("No scores yet"))
  }

  test("who played is told with the place reached and the score") {
    assert(Standings.told(Leaderboard.of(List(played("Gaia", 900)))) == Seq("1. Gaia 900"))
  }

  test("the best score is told first") {
    val board = Leaderboard.of(List(played("Alex", 300), played("Matilde", 1200)))
    assert(Standings.told(board).head == "1. Matilde 1200")
  }

  test("only the places worth showing are told") {
    val many = List.tabulate(20)(n => played(s"player$n", n))
    assert(Standings.told(Leaderboard.of(many)).size == Standings.Places)
  }

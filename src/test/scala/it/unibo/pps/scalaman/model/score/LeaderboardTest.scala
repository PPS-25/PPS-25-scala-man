package it.unibo.pps.scalaman.model.score

import it.unibo.pps.scalaman.model.score.LeaderboardTestSupport.{board, result}
import it.unibo.pps.scalaman.utils.MonoidLawsTest.lawsHold
import org.scalatest.funsuite.AnyFunSuite

class LeaderboardTest extends AnyFunSuite:
  private val leaderboards = List(
    Leaderboard.empty,
    board(result("solo", 100)),
    board(result("early", 100, at = 1), result("late", 100, at = 2)),
    board((1 to Leaderboard.Cap + 5).map(i => result(s"p$i", i * 10, at = i))*)
  )

  test("an empty leaderboard has no entries") {
    assert(Leaderboard.empty.entries.isEmpty)
  }

  test("recording an entry puts it on the leaderboard") {
    val entry = result("A", 100)
    assert(Leaderboard.empty.recordEntry(entry).entries.contains(entry))
  }

  test("entries are ordered by score, descending") {
    val filledLeaderboard = board(result("low", 100), result("high", 500))
    assert(filledLeaderboard.entries.head == result("high", 500))
  }

  test("if two entries are equal in scores, they are ordered by timestamp, earliest first") {
    val filled = board(result("later", 100, at = 2), result("sooner", 100, at = 1))
    assert(filled.entries.map(_.playerName) == List("sooner", "later"))
  }

  test("a board with a maximum of n scores, keeps the best n scores") {
    val tooMany = (1 to Leaderboard.Cap + 5).map(i => result("A", 1 * 10, at = i))
    assert(board(tooMany*).entries.size == Leaderboard.Cap)
  }

  test("recording a score tells you if a new hi-score has been hit")(pending)

  test("a leaderboard is a monoid") {
    lawsHold(leaderboards)
  }

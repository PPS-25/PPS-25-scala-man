package it.unibo.pps.scalaman.utils

import it.unibo.pps.scalaman.model.score.GameResult
import it.unibo.pps.scalaman.leaderboard.parser.GameResultCodec.given
import org.scalatest.funsuite.AnyFunSuite
import it.unibo.pps.scalaman.model.score.LeaderboardTestSupport.*

class GameResultCodecTest extends AnyFunSuite:

  private val encoder = summon[Encoder[GameResult]]
  private val decoder = summon[Decoder[GameResult]]

  private val results = List(
    result("A", 100),
    result("B", 0),
    result("A,B", 100),
    result("", 0),
    result("A", 500, at = 99)
  )

  test("a recorded result is persistent") {
    results.foreach(r => assert(decoder.decode(encoder.encode(r)).contains(r)))
  }

  test("a line with not enough fields is rejected") {
    assert(decoder.decode("100,2026-01-01T00:00:00Z").isEmpty)
  }

  test("a line must have a numeric score, otherwise it's rejected") {
    assert(decoder.decode("B,2026-01-01T00:00:00Z,A").isEmpty)
  }

  test("a line with an unparsable time is rejected") {
    assert(decoder.decode("100,MalformedTime,Name").isEmpty)
  }

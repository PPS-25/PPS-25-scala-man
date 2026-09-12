package it.unibo.pps.scalaman.model.score

import java.time.Instant

final case class GameResult(playerName: String, score: Int, achievedAt: Instant)

object GameResult:

  given Ordering[GameResult] =
    Ordering
      .by[GameResult, Int](_.score)
      .reverse
      .orElseBy(_.achievedAt.toEpochMilli)
      .orElseBy(_.playerName)

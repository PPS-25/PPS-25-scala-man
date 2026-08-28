package it.unibo.pps.scalaman.model.score

import java.time.Instant

final case class GameResult(playerName: String, score: Int, achievedAt: Instant)

object GameResult:

  /** Best score goes first. If two best scores are the same, order by time, oldest first. */
  given Ordering[GameResult] =
    Ordering
      .by[GameResult, Int](_.score)
      .reverse // descending for score
      .orElseBy(_.achievedAt.toEpochMilli) // ascending for time
      .orElseBy(_.playerName)

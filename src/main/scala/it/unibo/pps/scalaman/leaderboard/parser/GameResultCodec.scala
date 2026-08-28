package it.unibo.pps.scalaman.leaderboard.parser

import it.unibo.pps.scalaman.model.score.GameResult
import it.unibo.pps.scalaman.utils.{Decoder, Encoder}

import java.time.Instant
import scala.util.Try

object GameResultCodec:

  given Encoder[GameResult] with
    def encode(result: GameResult): String =
      s"${result.score},${result.achievedAt},${result.playerName}"

  given Decoder[GameResult] with
    def decode(text: String): Option[GameResult] =
      text.split(",", 3) match
        case Array(score, instant, name) =>
          for
            score <- score.toIntOption
            instant <- Try(Instant.parse(instant)).toOption
          yield GameResult(playerName = name, score = score, achievedAt = instant)
        case _ => None

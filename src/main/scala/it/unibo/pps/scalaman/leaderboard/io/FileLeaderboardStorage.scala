package it.unibo.pps.scalaman.leaderboard.io

import it.unibo.pps.scalaman.model.score.{
  GameResult,
  Leaderboard,
  LeaderboardError,
  LeaderboardStorage
}
import it.unibo.pps.scalaman.leaderboard.parser.GameResultCodec.given
import it.unibo.pps.scalaman.utils.{Decoder, Encoder}

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

case class FileLeaderboardStorage(path: Path) extends LeaderboardStorage:

  private val encoder = summon[Encoder[GameResult]]
  private val decoder = summon[Decoder[GameResult]]

  /** Loads the content of the storage path. If empty, creates a new leaderboard.
    */
  override def load(): Either[LeaderboardError, Leaderboard] =
    if !Files.exists(path) then Right(Leaderboard.empty)
    else readText.flatMap(decodeAll)

  override def save(leaderboard: Leaderboard): Either[LeaderboardError, Unit] =
    writeText(leaderboard.entries.map(encoder.encode).mkString("\n"))

  /** Parses the file into a string containing the game results, or an error if the read failed.
    */
  private def readText: Either[LeaderboardError, String] =
    try Right(Files.readString(path, StandardCharsets.UTF_8))
    catch case err: IOException => Left(LeaderboardError.ReadFailed(path, err.getMessage))

  /** Decode the parsed string into either a leaderboard, if no malformed lines were encountered, or
    * the first error encountered.
    */
  private def decodeAll(text: String): Either[LeaderboardError, Leaderboard] =
    val decoded = text.linesIterator
      .filter(_.nonEmpty)
      .toList
      .map(line => decoder.decode(line).toRight(LeaderboardError.Malformed(line)))
    decoded
      .collectFirst { case Left(err) => err }
      .toLeft(Leaderboard.of(decoded.collect { case Right(gr) => gr }))

  /** Writes the leaderboard at the desired path. If the path is in one or multiple directories, and
    * those do not exist yet, they are created. If an error is encountered, returns the error.
    */
  private def writeText(text: String): Either[LeaderboardError, Unit] = {
    try
      Option(path.getParent).foreach(Files.createDirectories(_))
      Files.writeString(path, text)
      Right(())
    catch case err: IOException => Left(LeaderboardError.WriteFailed(path, err.getMessage))
  }

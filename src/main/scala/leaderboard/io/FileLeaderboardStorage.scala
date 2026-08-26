package leaderboard.io

import it.unibo.pps.scalaman.model.score.{Leaderboard, LeaderboardError, LeaderboardStorage}

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

case class FileLeaderboardStorage(path: Path) extends LeaderboardStorage:

  override def load(): Either[LeaderboardError, Leaderboard] = ???
//    if !Files.exists(path) then Left(LeaderboardError.FileNotFound(path)) else
//      try Files.readString(path, StandardCharsets.UTF_8)
//      catch case err: IOException => Left(LeaderboardError.ReadException(path, err.getMessage))
//      decode()

  override def save(leaderboard: Leaderboard): Either[LeaderboardError, Unit] = ???
  // encode()

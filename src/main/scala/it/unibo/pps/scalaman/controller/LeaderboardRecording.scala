package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.score.{GameResult, LeaderboardError, LeaderboardStorage}

final case class LeaderboardRecording[S](
    result: S => Option[GameResult],
    storage: LeaderboardStorage
):
  def recording(state: S): Either[LeaderboardError, Unit] =
    result(state).fold(Right(()))(r => storage.load().map(_.recordEntry(r)).flatMap(storage.save))

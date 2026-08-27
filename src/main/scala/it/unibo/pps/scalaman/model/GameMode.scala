package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.collectibles.Collectibles

import scala.concurrent.duration.{Duration, FiniteDuration}

/** Rules that determine the current outcome of a game mode from immutable game state. */
trait GameMode:
  def status(
      progress: LevelProgress,
      collectibles: Collectibles,
      clock: GameClock
  ): GameState

object GameMode:

  /** Standard mode: collect every standard item while keeping at least one life. */
  case object Normal extends GameMode:
    def status(
        progress: LevelProgress,
        collectibles: Collectibles,
        clock: GameClock
    ): GameState =
      if progress.isOver then GameState.Defeat
      else if collectibles.isLevelComplete then GameState.Victory
      else GameState.Running

  /** Mode that requires the level to be completed strictly before its time limit. */
  final case class Timed(limit: FiniteDuration) extends GameMode:
    require(limit > Duration.Zero, "a timed mode must have a positive limit")

    def status(
        progress: LevelProgress,
        collectibles: Collectibles,
        clock: GameClock
    ): GameState =
      if clock.elapsed >= limit then GameState.Defeat
      else Normal.status(progress, collectibles, clock)

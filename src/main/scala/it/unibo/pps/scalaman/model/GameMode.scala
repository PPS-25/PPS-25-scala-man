package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.collectibles.Collectibles

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

/** Rules that determine the current outcome of a game mode from immutable game state. */
trait GameMode:
  def status(
      progress: LevelProgress,
      collectibles: Collectibles,
      clock: GameClock
  ): GameState

  /** The time enemies experience during an update at the current game time. */
  def enemyDelta(delta: FiniteDuration, clock: GameClock): FiniteDuration = delta

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

  /** Mode with no collectible-completion objective and progressively faster enemies. */
  final case class Survival(
      difficultyEvery: FiniteDuration = 30.seconds,
      maximumSpeedMultiplier: Long = 5
  ) extends GameMode:
    require(difficultyEvery > Duration.Zero, "difficulty must increase after a positive duration")
    require(
      maximumSpeedMultiplier > 0,
      "the maximum enemy speed multiplier must be positive"
    )

    def status(
        progress: LevelProgress,
        collectibles: Collectibles,
        clock: GameClock
    ): GameState =
      if progress.isOver then GameState.Defeat else GameState.Running

    override def enemyDelta(delta: FiniteDuration, clock: GameClock): FiniteDuration =
      val difficultyLevel = clock.elapsed.toNanos / difficultyEvery.toNanos
      val multiplier = (difficultyLevel + 1).min(maximumSpeedMultiplier)
      delta * multiplier

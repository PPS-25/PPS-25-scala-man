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

  /** The interval between enemy steps at the current game time. */
  def enemyStepInterval(standard: FiniteDuration, clock: GameClock): FiniteDuration = standard

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
      minimumEnemyStepInterval: FiniteDuration = 100.millis
  ) extends GameMode:
    require(difficultyEvery > Duration.Zero, "difficulty must increase after a positive duration")
    require(
      minimumEnemyStepInterval > Duration.Zero,
      "the minimum enemy step interval must be positive"
    )

    def status(
        progress: LevelProgress,
        collectibles: Collectibles,
        clock: GameClock
    ): GameState =
      if progress.isOver then GameState.Defeat else GameState.Running

    override def enemyStepInterval(
        standard: FiniteDuration,
        clock: GameClock
    ): FiniteDuration =
      val difficultyLevel = clock.elapsed.toNanos / difficultyEvery.toNanos
      val accelerated = standard / (difficultyLevel + 1)
      if accelerated < minimumEnemyStepInterval then minimumEnemyStepInterval else accelerated

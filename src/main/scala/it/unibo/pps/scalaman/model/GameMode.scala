package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.collectibles.Collectibles
import it.unibo.pps.scalaman.model.score.ScoringEvent.{RemainingLives, RemainingTime, WavesSurvived}
import it.unibo.pps.scalaman.model.score.{ScoringEvent, ScoringRule}

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

/** Rules that determine the current outcome of a game mode from immutable game state. */
trait GameMode:
  def status(
      progress: LevelProgress,
      collectibles: Collectibles,
      clock: GameClock
  ): GameState

  /** The scoring rule that the game mode uses. */
  def scoringRule: ScoringRule = ScoringRule.standardScoring

  /** The time enemies experience during an update at the current game time. */
  def enemyDelta(delta: FiniteDuration, clock: GameClock): FiniteDuration = delta

  /** How long a game has left, for the modes that run against a clock. */
  def timeLeft(clock: GameClock): Option[FiniteDuration] = None

  /** The final scoring event to be evaluated at the end of a game. It depends on the game mode. */
  def finalAward(progress: LevelProgress, clock: GameClock): ScoringEvent

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

    def finalAward(progress: LevelProgress, clock: GameClock): ScoringEvent = RemainingLives(
      progress.lives
    )

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

    override def timeLeft(clock: GameClock): Option[FiniteDuration] =
      Some((limit - clock.elapsed).max(Duration.Zero))

    override def scoringRule: ScoringRule = ScoringRule.timedScoring

    def finalAward(progress: LevelProgress, clock: GameClock): ScoringEvent = RemainingTime(
      timeLeft(clock).getOrElse(Duration.Zero)
    )

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

    override def scoringRule: ScoringRule = ScoringRule.survivalScoring

    /** How many times the difficulty has increased. */
    def wavesSurvived(clock: GameClock): Long =
      clock.elapsed.toNanos / difficultyEvery.toNanos

    override def enemyDelta(delta: FiniteDuration, clock: GameClock): FiniteDuration =
      val difficultyLevel = wavesSurvived(clock)
      val multiplier = (difficultyLevel + 1).min(maximumSpeedMultiplier)
      delta * multiplier

    def finalAward(progress: LevelProgress, clock: GameClock): ScoringEvent = WavesSurvived(
      wavesSurvived(clock).toInt
    )

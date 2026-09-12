package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.collectibles.Collectibles
import it.unibo.pps.scalaman.model.score.ScoringEvent.{RemainingLives, RemainingTime, WavesSurvived}
import it.unibo.pps.scalaman.model.score.{ScoringEvent, ScoringRule}

import scala.concurrent.duration.{Duration, DurationInt, DurationLong, FiniteDuration}

trait GameMode:
  def status(
      progress: LevelProgress,
      collectibles: Collectibles,
      clock: GameClock
  ): GameState

  def scoringRule: ScoringRule = ScoringRule.standardScoring

  def enemyDelta(delta: FiniteDuration, clock: GameClock): FiniteDuration = delta

  def timeLeft(clock: GameClock): Option[FiniteDuration] = None

  def bonus(progress: LevelProgress, clock: GameClock, over: Boolean): Option[ScoringEvent] = None

object GameMode:

  case object Normal extends GameMode:
    def status(
        progress: LevelProgress,
        collectibles: Collectibles,
        clock: GameClock
    ): GameState =
      if progress.isOver then GameState.Defeat
      else if collectibles.isLevelComplete then GameState.Victory
      else GameState.Running

    override def bonus(
        progress: LevelProgress,
        clock: GameClock,
        over: Boolean
    ): Option[ScoringEvent] =
      Option.when(over)(ScoringEvent.RemainingLives(progress.lives))

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

    override def bonus(
        progress: LevelProgress,
        clock: GameClock,
        over: Boolean
    ): Option[ScoringEvent] =
      Some(RemainingTime(timeLeft(clock).getOrElse(Duration.Zero)))

  val MaximumSurvivalSpeedMultiplier: Double = 1.25

  final case class Survival(
      difficultyEvery: FiniteDuration = 30.seconds,
      maximumSpeedMultiplier: Double = MaximumSurvivalSpeedMultiplier
  ) extends GameMode:
    require(difficultyEvery > Duration.Zero, "difficulty must increase after a positive duration")
    require(
      maximumSpeedMultiplier >= 1 && maximumSpeedMultiplier <= MaximumSurvivalSpeedMultiplier,
      "the maximum enemy speed multiplier must keep enemies no faster than the player"
    )

    def status(
        progress: LevelProgress,
        collectibles: Collectibles,
        clock: GameClock
    ): GameState =
      if progress.isOver then GameState.Defeat else GameState.Running

    override def scoringRule: ScoringRule = ScoringRule.survivalScoring

    override def bonus(
        progress: LevelProgress,
        clock: GameClock,
        over: Boolean
    ): Option[ScoringEvent] =
      Some(ScoringEvent.WavesSurvived(wavesSurvived(clock).toInt))

    def wavesSurvived(clock: GameClock): Long =
      clock.elapsed.toNanos / difficultyEvery.toNanos

    def speedMultiplier(clock: GameClock): Double =
      (1 + wavesSurvived(clock) * SpeedIncreasePerWave).min(maximumSpeedMultiplier)

    override def enemyDelta(delta: FiniteDuration, clock: GameClock): FiniteDuration =
      (delta.toNanos * speedMultiplier(clock)).round.nanos

  private val SpeedIncreasePerWave = 0.2

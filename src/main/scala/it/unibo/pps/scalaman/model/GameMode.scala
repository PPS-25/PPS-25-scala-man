package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.collectibles.Collectibles
import it.unibo.pps.scalaman.model.score.ScoringEvent.{RemainingLives, RemainingTime, WavesSurvived}
import it.unibo.pps.scalaman.model.score.{ScoringEvent, ScoringRule}

import scala.concurrent.duration.{Duration, DurationInt, DurationLong, FiniteDuration}

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

  /** What the game mode awards on top of the points scored while playing. */
  def bonus(progress: LevelProgress, clock: GameClock, over: Boolean): Option[ScoringEvent] = None

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

    override def bonus(
        progress: LevelProgress,
        clock: GameClock,
        over: Boolean
    ): Option[ScoringEvent] =
      Option.when(over)(ScoringEvent.RemainingLives(progress.lives))

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

    override def bonus(
        progress: LevelProgress,
        clock: GameClock,
        over: Boolean
    ): Option[ScoringEvent] =
      Some(RemainingTime(timeLeft(clock).getOrElse(Duration.Zero)))

  /** Mode with no collectible-completion objective and progressively faster enemies. */
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

    /** How many times the difficulty has increased. */
    def wavesSurvived(clock: GameClock): Long =
      clock.elapsed.toNanos / difficultyEvery.toNanos

    /** The gradual speed increase assigned to the wave reached at this point in the game. */
    def speedMultiplier(clock: GameClock): Double =
      (1 + wavesSurvived(clock) * SpeedIncreasePerWave).min(maximumSpeedMultiplier)

    override def enemyDelta(delta: FiniteDuration, clock: GameClock): FiniteDuration =
      (delta.toNanos * speedMultiplier(clock)).round.nanos

  /** Each survival wave raises speed by a noticeable but survivable amount. */
  private val SpeedIncreasePerWave = 0.2

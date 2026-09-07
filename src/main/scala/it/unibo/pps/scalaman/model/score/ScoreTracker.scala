package it.unibo.pps.scalaman.model.score

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

/** Types of events that award points.
  */
enum ScoringEvent:
  case BasicItem
  case BonusItem
  case RemainingLives(lives: Int)
  case EnemyKill
  case RemainingTime(left: FiniteDuration)
  case WavesSurvived(waves: Int)

trait ScoringRule:
  def awardedPoints(event: ScoringEvent, combo: Int = 1): Int

/** The amount of points that are awarded for each scoring event.
  */
object ScoringRule:
  private val BasicItemPoints = 50
  private val BonusItemPoints = 100
  private val RemainingLifeBonus = 500
  private val EnemyKillBasePoints = 200
  private val ComboFactor = 2
  private val PointsPerRemainingSecond = 1
  private val PointsPerWaveSurvived = 1

  val standardScoring: ScoringRule =
    (event: ScoringEvent, combo: Int) =>
      event match
        case ScoringEvent.BasicItem             => BasicItemPoints
        case ScoringEvent.BonusItem             => BonusItemPoints
        case ScoringEvent.RemainingLives(lives) => lives * RemainingLifeBonus
        case ScoringEvent.EnemyKill             => EnemyKillBasePoints * comboMultiplier(combo)
        case _                                  => 0

  val timedScoring: ScoringRule =
    (event: ScoringEvent, combo: Int) =>
      event match
        case ScoringEvent.RemainingTime(left) => left.toSeconds.toInt * PointsPerRemainingSecond
        case _                                => 0

  val survivalScoring: ScoringRule =
    (event: ScoringEvent, combo: Int) =>
      event match
        case ScoringEvent.WavesSurvived(waves) => waves * PointsPerWaveSurvived
        case _                                 => 0

  private def comboMultiplier(combo: Int): Int = {
    require(combo >= 1, "combo has to be at least of 1")
    List.fill(combo - 1)(ComboFactor).product
  }

/** A tracker to store the score of the game.
  */
final case class ScoreTracker(currentScore: Int = 0, combo: Int = 0):

  /** Increase the score. If the event was an enemy kill, increase the combo as well.
    */
  def increaseScore(event: ScoringEvent)(using rule: ScoringRule): ScoreTracker =
    val newCombo = increaseCombo(event, combo)
    ScoreTracker(currentScore + rule.awardedPoints(event, newCombo), newCombo)

  private def increaseCombo(event: ScoringEvent, combo: Int): Int = event match {
    case ScoringEvent.EnemyKill => combo + 1
    case _                      => combo
  }

  def resetCombo: ScoreTracker = copy(combo = 0)

  /** Create a persistent game result.
    */
  def toResult(playerName: String, remainingLives: Int, achievedAt: Instant)(using
      ScoringRule
  ): GameResult =
    GameResult(
      playerName,
      increaseScore(ScoringEvent.RemainingLives(remainingLives)).currentScore,
      achievedAt
    )

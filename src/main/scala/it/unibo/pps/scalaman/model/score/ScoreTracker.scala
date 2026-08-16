package it.unibo.pps.scalaman.model.score

import it.unibo.pps.scalaman.model.score.ScoringEvent.EnemyKill

/** Types of events that award points.
  */
enum ScoringEvent:
  case BasicItem
  case BonusItem
  case RemainingLives(lives: Int)
  case EnemyKill

/** The amount of points that are awarded for each scoring event.
  */
object ScoringRule:
  def awardedPoints(event: ScoringEvent, combo: Int = 1): Int = event match
    case ScoringEvent.BasicItem             => 50
    case ScoringEvent.BonusItem             => 100
    case ScoringEvent.RemainingLives(lives) => lives * 500
    case ScoringEvent.EnemyKill             => 200 * (1 << (combo - 1))

/** A tracker to store the score of the game.
  */
final case class ScoreTracker(currentScore: Int = 0, combo: Int = 0):

  /** Increase the score. If the event was an enemy kill, increase the combo as well.
    */
  def increaseScore(event: ScoringEvent): ScoreTracker =
    val newCombo = increaseCombo(event, combo)
    ScoreTracker(currentScore + ScoringRule.awardedPoints(event), combo)

  private def increaseCombo(event: ScoringEvent, combo: Int): Int = event match {
    case ScoringEvent.EnemyKill => combo + 1
    case _                      => combo
  }

  def resetCombo: ScoreTracker = copy(combo = 0)

  /** Create a persistent game result.
    */
  def toResult(playerName: String, remainingLives: Int): GameResult =
    GameResult(playerName, increaseScore(ScoringEvent.RemainingLives(remainingLives)).currentScore)

final case class GameResult(playerName: String = "Player1", score: Int = 0)

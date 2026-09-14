package it.unibo.pps.scalaman.model.score

import it.unibo.pps.scalaman.model.score.ScoringEvent.{
  BasicItem,
  EnemyKill,
  RemainingLives,
  WavesSurvived
}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class ScoringRuleTest extends AnyFunSuite:

  test("a combo must have started before it can be counted") {
    assertThrows[IllegalArgumentException](ScoringRule.standardScoring.awardedPoints(EnemyKill, 0))
  }

  test("in a timed game the seconds left are worth points and nothing else is") {
    val rule = ScoringRule.timedScoring
    assert(rule.awardedPoints(ScoringEvent.RemainingTime(40.seconds)) == 40)
    assert(rule.awardedPoints(BasicItem) == 0)
    assert(rule.awardedPoints(EnemyKill) == 0)
    assert(rule.awardedPoints(RemainingLives(3)) == 0)
  }

  test("in a survival game the waves survived are worth points and nothing else is") {
    val rule = ScoringRule.survivalScoring
    assert(rule.awardedPoints(ScoringEvent.WavesSurvived(3)) == 3)
    assert(rule.awardedPoints(BasicItem) == 0)
    assert(rule.awardedPoints(EnemyKill) == 0)
    assert(rule.awardedPoints(RemainingLives(3)) == 0)
  }

  test("the standard rule does not award points for time or wave survived") {
    val rule = ScoringRule.standardScoring
    assert(rule.awardedPoints(ScoringEvent.RemainingTime(40.seconds)) == 0)
    assert(rule.awardedPoints(WavesSurvived(3)) == 0)
  }

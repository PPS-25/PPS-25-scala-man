package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.GameState
import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.effects.BonusEffect.{Invulnerability, SlowDown}
import org.scalatest.funsuite.AnyFunSuite

class StatusBarTest extends AnyFunSuite:

  private def bar(lives: Int, remaining: Int, applied: Set[BonusEffect] = Set.empty) =
    StatusBar(lives, remaining, applied, GameState.Running)

  test("the lives left are told on their own, to be shown apart") {
    assert(bar(lives = 3, remaining = 1).livesDescribed == "Lives 3")
  }

  test("what is left to pick up is told") {
    assert(bar(3, 1).progressDescribed == "Left 1")
  }

  test("an applied effect is told beside what is left") {
    assert(bar(2, 4, Set(Invulnerability)).progressDescribed == "Left 4 | Invulnerability")
  }

  test("every applied effect is told, always in the same order") {
    val told = bar(1, 1, Set(SlowDown, Invulnerability)).progressDescribed
    assert(told.endsWith("Invulnerability, SlowDown"))
  }

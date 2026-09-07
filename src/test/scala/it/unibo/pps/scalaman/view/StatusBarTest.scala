package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.GameState
import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.effects.BonusEffect.{Invulnerability, SlowDown}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

class StatusBarTest extends AnyFunSuite:

  private def bar(
      lives: Int = 3,
      remaining: Int = 1,
      applied: Set[BonusEffect] = Set.empty,
      score: Int = 0,
      elapsed: FiniteDuration = Duration.Zero
  ) = StatusBar(lives, remaining, applied, GameState.Running, score, elapsed)

  test("the lives left and the score are told together") {
    assert(bar(lives = 3, score = 1200).playerDescribed == "Lives 3 | Score 1200")
  }

  test("the time played is told in minutes and seconds") {
    assert(bar(elapsed = 95.seconds).levelDescribed == "01:35 | Left 1")
  }

  test("minutes and seconds are always told with two figures") {
    assert(bar(elapsed = 5.seconds).levelDescribed.startsWith("00:05"))
  }

  test("a long game is still told in minutes") {
    assert(bar(elapsed = 3661.seconds).levelDescribed.startsWith("61:01"))
  }

  test("an applied effect is told beside what is left") {
    assert(
      bar(remaining = 4, applied = Set(Invulnerability)).levelDescribed.endsWith("Invulnerability")
    )
  }

  test("every applied effect is told, always in the same order") {
    val told = bar(applied = Set(SlowDown, Invulnerability)).levelDescribed
    assert(told.endsWith("Invulnerability, SlowDown"))
  }

package it.unibo.pps.scalaman.model

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class ModeTuningTest extends AnyFunSuite:

  private val tuning = summon[ModeTuning]

  test("the normal choice is played by the standard rules") {
    assert(tuning.of(ModeChoice.Normal) == GameMode.Normal)
  }

  test("a game against the clock is given two minutes") {
    assert(tuning.of(ModeChoice.Timed) == GameMode.Timed(2.minutes))
  }

  test("a game of survival is played by the rules that never let it be won") {
    assert(tuning.of(ModeChoice.Survival) == GameMode.Survival())
  }

  test("no two choices stand for the same rules") {
    assert(ModeChoice.values.map(tuning.of).distinct.length == ModeChoice.values.length)
  }

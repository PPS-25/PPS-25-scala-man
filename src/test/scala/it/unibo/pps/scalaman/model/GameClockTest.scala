package it.unibo.pps.scalaman.model

import org.scalatest.funsuite.AnyFunSuite
import scala.concurrent.duration.*

class GameClockTest extends AnyFunSuite:
  test("A new clock should have 0 elapsed time") {
    val clock = GameClock()
    assert(clock.elapsed == Duration.Zero)
  }

  test("A new clock should have 1 elapsed time after advancing once") {
    val clock = GameClock().advance(100.millis)
    assert(clock.elapsed == 100.millis)
  }

  test("A clock should accumulate elapsed time") {
    val clock = GameClock().advance(100.millis).advance(100.millis)
    assert(clock.elapsed == 200.millis)
  }

  test("A clock can't advance by a negative delta") {
    assertThrows[IllegalArgumentException] {
      GameClock().advance(-100.millis)
    }
  }

package it.unibo.pps.scalaman.model

import org.scalatest.funsuite.AnyFunSuite

class LevelProgressTest extends AnyFunSuite:
  private val progress = LevelProgress.initial

  test("a level starts with three lives") {
    assert(progress.lives == 3)
  }

  test("losing a life leaves one less") {
    assert(progress.lose.lives == progress.lives - 1)
  }

  test("a level is not over while the player has lives left") {
    assert(!progress.lose.isOver)
  }

  test("a level is over once the last life is lost") {
    assert(LevelProgress(1).lose.isOver)
  }

  test("losing a life when none is left leaves the player with none") {
    assert(LevelProgress(0).lose.lives == 0)
  }

  test("a level cannot start with a negative number of lives") {
    assertThrows[IllegalArgumentException](LevelProgress(-1))
  }

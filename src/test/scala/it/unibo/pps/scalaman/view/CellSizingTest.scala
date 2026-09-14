package it.unibo.pps.scalaman.view

import org.scalatest.funsuite.AnyFunSuite

class CellSizingTest extends AnyFunSuite:

  private def board(height: Int, width: Int): Board =
    Board(Vector.fill(height)(Vector.fill(width)(Sprite.Floor)))

  private val screen = ScreenSize(1600, 1000)

  test("a maze takes up most of the height of the screen") {
    val drawn = CellSizing.fitting(board(height = 20, width = 10), screen) * 20
    assert(drawn <= screen.height * 0.8 && drawn > screen.height * 0.6)
  }

  test("a maze wider than the screen is held back by the width") {
    val drawn = CellSizing.fitting(board(height = 5, width = 60), screen) * 60
    assert(drawn <= screen.width)
  }

  test("a position is never drawn too small to see") {
    assert(CellSizing.fitting(board(height = 400, width = 400), screen) == CellSizing.Smallest)
  }

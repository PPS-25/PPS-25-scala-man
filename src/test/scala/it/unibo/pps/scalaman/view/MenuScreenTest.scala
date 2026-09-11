package it.unibo.pps.scalaman.view

import org.scalatest.funsuite.AnyFunSuite

class MenuScreenTest extends AnyFunSuite:

  test("a player name is limited to the length the menu can show") {
    assert(MenuScreen.limitedName("a" * 25) == "a" * 24)
  }

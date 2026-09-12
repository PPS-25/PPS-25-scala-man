package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.GameState.{Defeat, Running, Victory}
import org.scalatest.funsuite.AnyFunSuite

class GameStateTest extends AnyFunSuite:

  test("Victory and defeat are terminal states") {
    assert(Victory.isTerminal)
    assert(Defeat.isTerminal)
    assert(!Running.isTerminal)
  }

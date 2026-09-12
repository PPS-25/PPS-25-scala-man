package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.controller.CommandMapper
import org.scalatest.funsuite.AnyFunSuite

class CommandMapperTest extends AnyFunSuite:

  test("the left arrow and A keys map to a left direction") {
    assert(CommandMapper.toDir("A") == Option(Direction.Left))
    assert(CommandMapper.toDir("LEFT") == Option(Direction.Left))
  }

  test("the right arrow and D keys map to a right direction") {
    assert(CommandMapper.toDir("D") == Option(Direction.Right))
    assert(CommandMapper.toDir("RIGHT") == Option(Direction.Right))
  }

  test("the up arrow and W keys map to an up direction") {
    assert(CommandMapper.toDir("W") == Option(Direction.Up))
    assert(CommandMapper.toDir("UP") == Option(Direction.Up))
  }

  test("the down arrow and S keys map to a down direction") {
    assert(CommandMapper.toDir("S") == Option(Direction.Down))
    assert(CommandMapper.toDir("DOWN") == Option(Direction.Down))
  }

  test("any other key does not map to a direction") {
    assert(CommandMapper.toDir("F").isEmpty)
  }

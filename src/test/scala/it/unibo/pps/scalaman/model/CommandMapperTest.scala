package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.controller.CommandMapper
import org.scalatest.funsuite.AnyFunSuite
import scalafx.scene.input.KeyCode

class CommandMapperTest extends AnyFunSuite:

  test("the left arrow and A keys map to a left direction") {
    assert(CommandMapper.toDir(KeyCode.A) == Option(Direction.Left))
    assert(CommandMapper.toDir(KeyCode.Left) == Option(Direction.Left))
  }

  test("the right arrow and D keys map to a right direction") {
    assert(CommandMapper.toDir(KeyCode.D) == Option(Direction.Right))
    assert(CommandMapper.toDir(KeyCode.Right) == Option(Direction.Right))
  }

  test("the up arrow and W keys map to an up direction") {
    assert(CommandMapper.toDir(KeyCode.W) == Option(Direction.Up))
    assert(CommandMapper.toDir(KeyCode.Up) == Option(Direction.Up))
  }

  test("the down arrow and S keys map to a down direction") {
    assert(CommandMapper.toDir(KeyCode.S) == Option(Direction.Down))
    assert(CommandMapper.toDir(KeyCode.Down) == Option(Direction.Down))
  }

  test("any other key does not map to a direction") {
    assert(CommandMapper.toDir(KeyCode.F).isEmpty)
  }

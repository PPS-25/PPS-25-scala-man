package it.unibo.pps.scalaman.view

import org.scalatest.funsuite.AnyFunSuite
import it.unibo.pps.scalaman.model.Direction

class SpriteImagesTest extends AnyFunSuite:

  test("the picture of every sprite is there to be drawn") {
    val missing = Sprite.All.map(SpriteImages.fileOf).filter(getClass.getResource(_) == null)
    assert(missing.isEmpty)
  }

  test("each player direction reuses the same source picture before it is rotated on the board") {
    assert(
      Direction.values.map(direction => SpriteImages.fileOf(Sprite.Player(Mouth.Open, direction))).toSet ==
        Set("/scalaman1.png")
    )
  }

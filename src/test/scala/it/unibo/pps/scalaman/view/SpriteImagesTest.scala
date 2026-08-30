package it.unibo.pps.scalaman.view

import org.scalatest.funsuite.AnyFunSuite

class SpriteImagesTest extends AnyFunSuite:

  test("the picture of every sprite is there to be drawn") {
    val missing = Sprite.All.map(SpriteImages.fileOf).filter(getClass.getResource(_) == null)
    assert(missing.isEmpty)
  }

  test("what must be told apart is drawn with its own picture") {
    assert(Sprite.All.map(SpriteImages.fileOf).size == Sprite.All.size)
  }

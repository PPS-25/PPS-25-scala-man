package it.unibo.pps.scalaman.model

enum Direction(val dx: Int, val dy: Int):
  case Up extends Direction(0, -1)
  case Down extends Direction(0, 1)
  case Left extends Direction(-1, 0)
  case Right extends Direction(1, 0)

package it.unibo.pps.scalaman.model

final case class Position(row: Int, col: Int):
  def +(direction: Direction): Position =
    copy(row = row + direction.dy, col = col + direction.dx)

package it.unibo.pps.scalaman.model

/** A position on the map.
  * @param row
  *   the row on the map.
  * @param col
  *   the column on the map.
  */
final case class Position(row: Int, col: Int):

  /** Allows to easily compute adjacent positions by using the direction.
    * @param direction
    *   the direction in which to move from the starting position.
    */
  def +(direction: Direction): Position =
    copy(row = row + direction.dy, col = col + direction.dx)

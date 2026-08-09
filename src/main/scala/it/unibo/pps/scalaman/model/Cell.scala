package it.unibo.pps.scalaman.model

/** A cell of the map grid.
  * @param col
  *   the column.
  * @param row
  *   the row.
  */
final case class Cell(col: Int, row: Int)

extension (cell: Cell)
  /** Allows easy calculation of result of movement from a cell to its adjacent
    * one.
    * @param direction
    *   the direction of the adjacent cell.
    */
  def +(direction: Direction): Cell =
    cell.copy(col = cell.col + direction.dx, row = cell.row + direction.dy)

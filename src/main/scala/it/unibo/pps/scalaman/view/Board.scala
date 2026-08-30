package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.{Tile, ValidatedMap}

/** The maze as it is drawn: what stands still for the whole level. */
final case class Board(cells: Vector[Vector[Sprite]]):
  val height: Int = cells.length
  val width: Int = cells.headOption.fold(0)(_.length)

  def at(position: Position): Option[Sprite] =
    cells.lift(position.row).flatMap(_.lift(position.col))

object Board:

  /** The maze of a level. A tile that only marks where someone starts is drawn as floor: what
    * stands on it moves, and is drawn frame by frame.
    */
  def of(maze: ValidatedMap): Board =
    val pairs = pairedEnds(maze)
    Board(maze.raw.rows.zipWithIndex.map { case (row, rowIndex) =>
      row.zipWithIndex.map { case (tile, colIndex) =>
        spriteOf(tile, Position(rowIndex, colIndex), pairs)
      }
    })

  private def spriteOf(tile: Tile, position: Position, pairs: Map[Position, Int]): Sprite =
    tile match
      case Tile.Wall        => Sprite.Wall
      case Tile.Teleport(_) => pairs.get(position).fold(Sprite.Floor)(Sprite.Teleport.apply)
      case _                => Sprite.Floor

  private def pairedEnds(maze: ValidatedMap): Map[Position, Int] =
    maze.teleports.flatMap { case (pair, (entrance, exit)) =>
      Seq(entrance -> pair, exit -> pair)
    }

final case class ScreenSize(width: Double, height: Double)

/** How wide a position is drawn, so that a maze fills most of the screen whatever its shape. */
object CellSizing:

  /** How small a position can get before it is no longer worth looking at. */
  val Smallest: Double = 12.0

  private val OfTheHeight = 0.8
  private val OfTheWidth = 0.9
  private val LeftToTheStatus = 60.0

  /** Both bounds hold at once, hence the smaller of the two, and never below what can be seen. */
  def fitting(board: Board, screen: ScreenSize): Double =
    val byHeight = (screen.height * OfTheHeight - LeftToTheStatus) / board.height
    val byWidth = screen.width * OfTheWidth / board.width
    byHeight.min(byWidth).max(Smallest)

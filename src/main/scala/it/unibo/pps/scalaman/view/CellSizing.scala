package it.unibo.pps.scalaman.view

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

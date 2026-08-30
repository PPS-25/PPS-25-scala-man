package it.unibo.pps.scalaman.view

import scalafx.geometry.Insets
import scalafx.scene.Parent
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.control.Label
import scalafx.scene.layout.{BorderPane, StackPane}
import scalafx.stage.Screen

/** Draws a level on two layers: the maze once, because it stands still, and whoever moves over it
  * at every frame.
  */
final class GameBoard(board: Board, cellSize: Double):

  import GameBoard.*

  private val maze = new Canvas(board.width * cellSize, board.height * cellSize)
  private val entities = new Canvas(board.width * cellSize, board.height * cellSize)
  private val lives = told()
  private val progress = told()

  drawMaze()

  /** What to put on a scene to see the level. */
  val node: Parent = new BorderPane:
    top = new BorderPane:
      left = lives
      right = progress
    center = new StackPane:
      children = Seq(maze, entities)

  /** Draws a frame over the maze, which is left untouched. */
  def draw(frame: Frame): Unit =
    val gc = entities.graphicsContext2D
    gc.clearRect(0, 0, entities.width.value, entities.height.value)
    frame.entities.foreach(drawn => paint(gc, drawn.at, drawn.sprite))
    lives.text = frame.status.livesDescribed
    progress.text = frame.status.progressDescribed

  private def told(): Label = new Label(""):
    padding = Insets(SpacedBy)
    style = s"-fx-font-size: ${cellSize * TextOfCell}px; -fx-font-weight: bold;"

  // Walls and doors are transparent at the corners, so floor goes under every position.
  private def drawMaze(): Unit =
    val gc = maze.graphicsContext2D
    for
      (row, rowIndex) <- board.cells.zipWithIndex
      (sprite, colIndex) <- row.zipWithIndex
      spot = Spot(rowIndex, colIndex)
    do
      paint(gc, spot, Sprite.Floor)
      if sprite != Sprite.Floor then paint(gc, spot, sprite)

  private def paint(gc: GraphicsContext, spot: Spot, sprite: Sprite): Unit =
    gc.drawImage(
      SpriteImages.of(sprite),
      spot.col * cellSize,
      spot.row * cellSize,
      cellSize,
      cellSize
    )

object GameBoard:

  /** A board drawn as large as the screen it is played on allows. */
  def fittingScreen(board: Board): GameBoard =
    val bounds = Screen.primary.visualBounds
    GameBoard(board, CellSizing.fitting(board, ScreenSize(bounds.width, bounds.height)))

  private val SpacedBy = 10.0
  private val TextOfCell = 0.45

package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.app.Command
import scalafx.geometry.Insets
import scalafx.scene.Parent
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.control.Label
import scalafx.scene.control.Button
import scalafx.scene.layout.{BorderPane, StackPane, VBox}
import scalafx.stage.Screen

/** Draws a level on two layers: the maze once, because it stands still, and whoever moves over it
  * at every frame.
  */
final class GameBoard(board: Board, cellSize: Double, chosen: Command => Unit):

  import GameBoard.*

  private val maze = new Canvas(board.width * cellSize, board.height * cellSize)
  private val entities = new Canvas(board.width * cellSize, board.height * cellSize)
  private val lives = told()
  private val progress = told()
  private val hold = new Button("Pause"):
    onAction = _ => chosen(Command.Pause)
    style = Style.button
  private val veil = new VBox:
    alignment = scalafx.geometry.Pos.Center
    spacing = SpacedBy
    visible = false
    style = Style.veil

  drawMaze()

  /** What to put on a scene to see the level. */
  val node: Parent = new BorderPane:
    top = new BorderPane:
      left = lives
      center = hold
      right = progress
      style = Style.background
      padding = Insets(SpacedBy / 2)
    center = new StackPane:
      children = Seq(maze, entities, veil)

  /** Draws a frame over the maze, which is left untouched. */
  def draw(frame: Frame): Unit =
    val gc = entities.graphicsContext2D
    gc.clearRect(0, 0, entities.width.value, entities.height.value)
    frame.entities.foreach(drawn => paint(gc, drawn.at, drawn.sprite))
    lives.text = frame.status.playerDescribed
    progress.text = frame.status.levelDescribed

  // Rebuilding the veil at every frame would replace a button before its click is over.
  private var covered: Option[Overlay] = None

  /** Covers the board with what is read while the game is not being played, or uncovers it. */
  def cover(overlay: Option[Overlay]): Unit =
    if overlay != covered then
      covered = overlay
      veil.visible = overlay.isDefined
      hold.visible = overlay.isEmpty
      veil.children = overlay.fold(Seq.empty)(written)

  private def written(overlay: Overlay): Seq[scalafx.scene.Node] =
    val title = new Label(overlay.title):
      style = Style.text(cellSize * TitleOfCell)
    val lines = overlay.lines.map(line =>
      new Label(line):
        style = Style.text(cellSize * TextOfCell)
    )
    val choices = overlay.choices.map(command =>
      new Button(spelled(command)):
        style = Style.button
        onAction = _ => chosen(command)
    )
    title +: (lines ++ choices)

  private def spelled(command: Command): String = command match
    case Command.Restart     => "Play again"
    case Command.Resume      => "Resume"
    case Command.SaveAndQuit => "Save and quit"
    case Command.BackToMenu  => "Back to menu"
    case _                   => "Play"

  private def told(): Label = new Label(""):
    padding = Insets(SpacedBy)
    style = Style.text(cellSize * TextOfCell)

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
  def fittingScreen(board: Board, chosen: Command => Unit): GameBoard =
    val bounds = Screen.primary.visualBounds
    GameBoard(board, CellSizing.fitting(board, ScreenSize(bounds.width, bounds.height)), chosen)

  private val SpacedBy = 10.0
  private val TextOfCell = 0.3
  private val TitleOfCell = 0.85

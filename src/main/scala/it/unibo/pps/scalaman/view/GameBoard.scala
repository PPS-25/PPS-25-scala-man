package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.app.Command
import it.unibo.pps.scalaman.model.Direction
import scalafx.geometry.Insets
import scalafx.scene.Parent
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.control.Label
import scalafx.scene.control.Button
import scalafx.scene.layout.{BorderPane, StackPane, VBox}
import scalafx.stage.Screen

/** Draws a level on two layers: the maze once, because it stands still, and whoever moves over it
  * at every frame. Buttons emit [[Command]] values through `handleCommand`.
  */
final class GameBoard(board: Board, cellSize: Double, handleCommand: Command => Unit):

  import GameBoard.*

  private val mazeCanvas = new Canvas(board.width * cellSize, board.height * cellSize)
  private val entityCanvas = new Canvas(board.width * cellSize, board.height * cellSize)
  private val livesLabel = statusLabel()
  private val progressLabel = statusLabel()
  private val pauseButton = new Button("Pause"):
    onAction = _ => handleCommand(Command.Pause)
    style = Style.button
  private val overlayPane = new VBox:
    alignment = scalafx.geometry.Pos.Center
    spacing = SpacedBy
    visible = false
    style = Style.veil

  drawMaze()

  /** What to put on a scene to see the level. */
  val node: Parent = new BorderPane:
    top = new BorderPane:
      left = livesLabel
      center = pauseButton
      right = progressLabel
      style = Style.background
      padding = Insets(SpacedBy / 2)
    center = new StackPane:
      children = Seq(mazeCanvas, entityCanvas, overlayPane)

  /** Draws a frame over the maze, which is left untouched. */
  def draw(frame: Frame): Unit =
    val gc = entityCanvas.graphicsContext2D
    gc.clearRect(0, 0, entityCanvas.width.value, entityCanvas.height.value)
    frame.entities.foreach(drawn => paint(gc, drawn.at, drawn.sprite))
    livesLabel.text = frame.status.playerDescribed
    progressLabel.text = frame.status.levelDescribed

  // Rebuilding the veil at every frame would replace a button before its click is over.
  private var displayedOverlay: Option[Overlay] = None

  /** Covers the board with what is read while the game is not being played, or uncovers it. */
  def cover(overlay: Option[Overlay]): Unit =
    if overlay != displayedOverlay then
      displayedOverlay = overlay
      overlayPane.visible = overlay.isDefined
      pauseButton.visible = overlay.isEmpty
      overlayPane.children = overlay.fold(Seq.empty)(overlayNodes)

  private def overlayNodes(overlay: Overlay): Seq[scalafx.scene.Node] =
    val title = new Label(overlay.title):
      style = Style.text(Style.Banner)
    val lines = overlay.lines.map(line =>
      new Label(line):
        style = Style.text(Style.Reading)
    )
    val choices = overlay.choices.map(command =>
      new Button(spelled(command)):
        style = Style.button
        onAction = _ => handleCommand(command)
    )
    title +: (lines ++ choices)

  private def spelled(command: Command): String = command match
    case Command.Restart     => "Play again"
    case Command.Resume      => "Resume"
    case Command.SaveAndQuit => "Save and quit"
    case Command.BackToMenu  => "Back to menu"
    case _                   => "Play"

  private def statusLabel(): Label = new Label(""):
    padding = Insets(SpacedBy)
    style = Style.text(Style.Reading)

  // Walls and doors are transparent at the corners, so floor goes under every position.
  private def drawMaze(): Unit =
    val gc = mazeCanvas.graphicsContext2D
    for
      (row, rowIndex) <- board.cells.zipWithIndex
      (sprite, colIndex) <- row.zipWithIndex
      spot = Spot(rowIndex, colIndex)
    do
      paint(gc, spot, Sprite.Floor)
      if sprite != Sprite.Floor then paint(gc, spot, sprite)

  private def paint(gc: GraphicsContext, spot: Spot, sprite: Sprite): Unit =
    sprite match
      case Sprite.Player(_, facing) =>
        gc.save()
        gc.translate((spot.col + 0.5) * cellSize, (spot.row + 0.5) * cellSize)
        val (rotation, mirrored) = transformOf(facing)
        gc.rotate(rotation)
        if mirrored then gc.scale(-1, 1)
        gc.drawImage(SpriteImages.of(sprite), -cellSize / 2, -cellSize / 2, cellSize, cellSize)
        gc.restore()
      case _ =>
        gc.drawImage(
          SpriteImages.of(sprite),
          spot.col * cellSize,
          spot.row * cellSize,
          cellSize,
          cellSize
        )

  // Player pictures face right. Left is mirrored rather than rotated, so the character stays upright.
  private def transformOf(direction: Direction): (Double, Boolean) = direction match
    case Direction.Right => (0, false)
    case Direction.Down  => (90, false)
    case Direction.Left  => (0, true)
    case Direction.Up    => (270, false)

object GameBoard:

  /** A board drawn as large as the screen it is played on allows. */
  def fittingScreen(board: Board, handleCommand: Command => Unit): GameBoard =
    val bounds = Screen.primary.visualBounds
    GameBoard(
      board,
      CellSizing.fitting(board, ScreenSize(bounds.width, bounds.height)),
      handleCommand
    )

  private val SpacedBy = 10.0

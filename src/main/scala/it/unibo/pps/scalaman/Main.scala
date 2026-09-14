package it.unibo.pps.scalaman

import it.unibo.pps.scalaman.app.Command
import it.unibo.pps.scalaman.controller.{
  Application,
  ApplicationNotice,
  CommandMapper,
  GameFilesEnvironment,
  Playing
}
import it.unibo.pps.scalaman.view.{LevelView, RenderListener}
import it.unibo.pps.scalaman.model.ModeTuning
import it.unibo.pps.scalaman.model.effects.given
import it.unibo.pps.scalaman.model.map.ValidatedMap
import it.unibo.pps.scalaman.persistence.PropertiesGameSaveRepository
import it.unibo.pps.scalaman.view.{
  Board,
  Frame,
  GameBoard,
  MenuScreen,
  Overlay,
  Screen,
  StatusBar,
  Style
}
import scalafx.Includes.*
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control.Alert
import scalafx.scene.input.KeyEvent
import scalafx.scene.paint.Color

def applicationName: String = "scala-man"

/** JavaFX entry point and imperative UI shell. */
object Main extends JFXApp3:

  private var application = Application(
    GameFilesEnvironment.ofUser(PropertiesGameSaveRepository()),
    createGameRenderer,
    ModeTuning.standardModes
  )

  private var gameBoard: Option[GameBoard] = None
  private var displayedScreen: Option[Screen] = None

  override def start(): Unit =
    stage = new JFXApp3.PrimaryStage:
      title = applicationName
      maximized = true
      scene = new Scene:
        fill = Color.web(Style.Night)
        root = createMenu()
        // The filter runs before focused controls consume arrow keys.
        filterEvent(KeyEvent.KeyPressed) { (event: KeyEvent) => handleKeyPress(event) }
    AnimationTimer(onAnimationFrame).start()

  /** Entry point for every command emitted by the menu, board, or keyboard. */
  private def handleCommand(command: Command): Unit =
    applyApplicationState(application.handleCommand(command))

  /** Advances the application state on each JavaFX animation frame. */
  private def onAnimationFrame(now: Long): Unit =
    applyApplicationState(application.advancedToFrame(now))
    application.playing.foreach(updateGameOverlay)

  /** Makes a new immutable application state visible in the JavaFX shell. */
  private def applyApplicationState(next: Application): Unit =
    if application.playing.isDefined && next.playing.isEmpty then stage.scene().root = createMenu()
    application = next.noticed
    next.notice.foreach(showNotice)

  /** Creates the board for a level and returns the listener used to redraw each changed view. */
  private def createGameRenderer(maze: ValidatedMap): RenderListener[LevelView] =
    val board = GameBoard.fittingScreen(Board.of(maze), handleCommand)
    gameBoard = Some(board)
    stage.scene().root = board.node
    levelView => board.draw(Frame.of(levelView))

  // The overlay depends on loop and level state, so it lives outside the level projection.
  // Updating it only when the screen changes preserves the final score after a game ends.
  private def updateGameOverlay(playing: Playing): Unit =
    val screen = Screen.of(playing.loop, playing.status, playing.startingIn)
    if !displayedScreen.contains(screen) then
      displayedScreen = Some(screen)
      gameBoard.foreach(
        _.cover(Overlay.of(screen, StatusBar.of(LevelView.of(playing.session.level))))
      )

  private def createMenu(): scalafx.scene.Parent =
    MenuScreen(
      application.mazes,
      application.bestOn,
      application.playerName,
      application.savesFolder,
      handleCommand
    ).node

  /** Handles movement keys before focused controls use arrows for navigation. */
  private def handleKeyPress(event: KeyEvent): Unit =
    if CommandMapper.isPauseKey(event.code.toString) then handleCommand(Command.Pause)
    else
      for
        direction <- CommandMapper.toDir(event.code.toString)
        if application.acceptsDirectionInput
      do
        application = application.requestDirection(direction)
        event.consume()

  // A modal wait cannot be opened while an animation frame is being processed.
  private def showNotice(notice: ApplicationNotice): Unit =
    notice match
      case ApplicationNotice.Error(message) =>
        new Alert(Alert.AlertType.Error):
          title = applicationName
          headerText = "scala-man could not do that"
          contentText = message
        .show()
      case ApplicationNotice.Information(_) => ()

package it.unibo.pps.scalaman

import it.unibo.pps.scalaman.app.Command
import it.unibo.pps.scalaman.controller.{
  Application,
  ApplicationNotice,
  CommandMapper,
  GameFilesEnvironment,
  LevelView,
  Playing,
  RenderListener
}
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

/** The name the application is known by, on its window and in its messages. */
def applicationName: String = "scala-man"

/** The window: it draws what the application became, and hands it whatever whoever plays asks for.
  * Nothing is decided here, which is why nothing here is tested — see `Application`.
  */
object Main extends JFXApp3:

  private var application = Application(
    GameFilesEnvironment.ofUser(PropertiesGameSaveRepository()),
    showing,
    ModeTuning.standardModes
  )

  private var board: Option[GameBoard] = None
  private var veiled: Option[Screen] = None

  override def start(): Unit =
    stage = new JFXApp3.PrimaryStage:
      title = applicationName
      maximized = true
      scene = new Scene:
        fill = Color.web(Style.Night)
        root = menu
        // A filter, not a handler, and for the reason given on `steering`.
        filterEvent(KeyEvent.KeyPressed) { (event: KeyEvent) => steering(event) }
    AnimationTimer(framed).start()

  private def asked(command: Command): Unit = became(application.commanded(command))

  private def framed(now: Long): Unit =
    became(application.advancedToFrame(now))
    application.playing.foreach(covered)

  /** Whatever the application became: a game that ended goes back to the menu, and anything it has
    * to say is said once.
    */
  private def became(next: Application): Unit =
    if application.playing.isDefined && next.playing.isEmpty then stage.scene().root = menu
    application = next.noticed
    next.notice.foreach(announced)

  /** How a level of a maze is drawn: the board shown here, and the brush handed back to whoever
    * advances the game.
    */
  private def showing(maze: ValidatedMap): RenderListener[LevelView] =
    val drawn = GameBoard.fittingScreen(Board.of(maze), asked)
    board = Some(drawn)
    stage.scene().root = drawn.node
    view => drawn.draw(Frame.of(view))

  // The veil is what the loop and the level say together, so it goes on outside the projection.
  // Only when the screen changes: a game that ended would otherwise keep projecting its own score
  // for as long as its veil is read.
  private def covered(playing: Playing): Unit =
    val screen = Screen.of(playing.loop, playing.status, playing.startingIn)
    if !veiled.contains(screen) then
      veiled = Some(screen)
      board.foreach(
        _.cover(Overlay.of(screen, StatusBar.of(LevelView.of(playing.session.level))))
      )

  /** The menu as it is right now, so that a maze added while the game is open is offered as soon as
    * the menu comes back.
    */
  private def menu: scalafx.scene.Parent =
    MenuScreen(application.mazes, application.bestOn, application.playerName, asked).node

  /** What a key press asks for. Every control claims the arrows to move the focus and consumes
    * them, so a steer is read on the way down and, once taken, consumed in its turn.
    */
  private def steering(event: KeyEvent): Unit =
    if CommandMapper.isPauseKey(event.code) then asked(Command.Pause)
    else
      for
        direction <- CommandMapper.toDir(event.code)
        if application.steerable
      do
        application = application.steered(direction)
        event.consume()

  // Shown rather than waited on: a frame is being drawn, and a modal wait would refuse to open.
  private def announced(notice: ApplicationNotice): Unit =
    val (kind, header) = notice match
      case ApplicationNotice.Error(_) => (Alert.AlertType.Error, "scala-man could not do that")
      case ApplicationNotice.Information(_) => (Alert.AlertType.Information, "Result recorded")
    new Alert(kind):
      title = applicationName
      headerText = header
      contentText = notice.message
    .show()

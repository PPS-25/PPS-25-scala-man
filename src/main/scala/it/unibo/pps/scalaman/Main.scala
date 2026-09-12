package it.unibo.pps.scalaman

import it.unibo.pps.scalaman.app.Command
import it.unibo.pps.scalaman.controller.{
  Application,
  ApplicationNotice,
  CommandMapper,
  GameFilesEnvironment,
  Playing
}
import it.unibo.pps.scalaman.presentation.{LevelView, RenderListener}
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
        filterEvent(KeyEvent.KeyPressed) { (event: KeyEvent) => steering(event) }
    AnimationTimer(framed).start()

  private def asked(command: Command): Unit = became(application.commanded(command))

  private def framed(now: Long): Unit =
    became(application.advancedToFrame(now))
    application.playing.foreach(covered)

  private def became(next: Application): Unit =
    if application.playing.isDefined && next.playing.isEmpty then stage.scene().root = menu
    application = next.noticed
    next.notice.foreach(announced)

  private def showing(maze: ValidatedMap): RenderListener[LevelView] =
    val drawn = GameBoard.fittingScreen(Board.of(maze), asked)
    board = Some(drawn)
    stage.scene().root = drawn.node
    view => drawn.draw(Frame.of(view))

  private def covered(playing: Playing): Unit =
    val screen = Screen.of(playing.loop, playing.status, playing.startingIn)
    if !veiled.contains(screen) then
      veiled = Some(screen)
      board.foreach(
        _.cover(Overlay.of(screen, StatusBar.of(LevelView.of(playing.session.level))))
      )

  private def menu: scalafx.scene.Parent =
    MenuScreen(
      application.mazes,
      application.bestOn,
      application.playerName,
      application.savesFolder,
      asked
    ).node

  private def steering(event: KeyEvent): Unit =
    if CommandMapper.isPauseKey(event.code.toString) then asked(Command.Pause)
    else
      for
        direction <- CommandMapper.toDir(event.code.toString)
        if application.steerable
      do
        application = application.steered(direction)
        event.consume()

  private def announced(notice: ApplicationNotice): Unit =
    notice match
      case ApplicationNotice.Error(message) =>
        new Alert(Alert.AlertType.Error):
          title = applicationName
          headerText = "scala-man could not do that"
          contentText = message
        .show()
      case ApplicationNotice.Information(_) => ()

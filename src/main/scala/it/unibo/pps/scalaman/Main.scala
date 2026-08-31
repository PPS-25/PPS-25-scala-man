package it.unibo.pps.scalaman

import it.unibo.pps.scalaman.controller.{
  CommandMapper,
  GameSession,
  LeaderboardRecording,
  LevelView
}
import it.unibo.pps.scalaman.leaderboard.io.FileLeaderboardStorage
import it.unibo.pps.scalaman.map.io.MapLoader
import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.LevelState
import it.unibo.pps.scalaman.model.map.ValidatedMap
import it.unibo.pps.scalaman.view.{Board, Frame, GameBoard}
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.Parent
import scalafx.scene.input.KeyCode
import scalafx.Includes.*

import java.nio.file.{Path, Paths}

/** Name shown while the game loop is not yet in place. */
def applicationName: String = "scala-man"

object Main extends JFXApp3:

  private val DefaultMap: Path = Paths.get("maps", "level2.txt")
  private val LeaderboardFile: Path = Paths.get("data", "leaderboard.csv")
  private val PlayerName = "Player"

  private def mazeAt(path: Path): Either[String, ValidatedMap] =
    for
      text <- MapLoader.load(path).left.map(_.toString)
      raw <- MapParser.parse(text).left.map(_.mkString(", "))
      maze <- MapValidator.validate(raw).left.map(_.mkString(", "))
    yield maze

  override def start(): Unit =
    mazeAt(DefaultMap) match
      case Left(err)   => sys.error(s"could not start scala-man: $err")
      case Right(maze) => play(maze)

  private def play(maze: ValidatedMap): Unit =
    val board = GameBoard.fittingScreen(Board.of(maze))
    val recording = LeaderboardRecording[LevelState](
      _.result(PlayerName),
      FileLeaderboardStorage(LeaderboardFile)
    )
    var session = GameSession.starting(LevelState.from(maze), view => board.draw(Frame.of(view)))
    stage = window(board.node, key => session = keyed(session, key))
    AnimationTimer { now =>
      val wasOver = session.isOver
      session = session.advancedToFrame(now)
      if !wasOver && session.isOver then
        recording.recording(session.level).left.foreach(e => println(s"score not saved: $e"))
    }.start()

  private def window(root: Parent, onKey: KeyCode => Unit): JFXApp3.PrimaryStage =
    new JFXApp3.PrimaryStage:
      title = "scala-man"
      scene = new Scene(root):
        onKeyPressed = event => onKey(event.code)

  private def keyed(session: GameSession, key: KeyCode): GameSession =
    if CommandMapper.isPauseKey(key) then session.togglePause
    else CommandMapper.toDir(key).fold(session)(session.requestingDirection)

package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.app.{Command, MapName, Played, PlayerName}
import it.unibo.pps.scalaman.model.LevelTestSupport
import it.unibo.pps.scalaman.model.collectibles.Collectibles
import it.unibo.pps.scalaman.model.effects.given
import it.unibo.pps.scalaman.model.map.ValidatedMap
import it.unibo.pps.scalaman.model.score.{GameResult, Leaderboard}
import it.unibo.pps.scalaman.model.{
  Direction,
  GameMode,
  GameState,
  LevelState,
  ModeChoice,
  ModeTuning
}
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Path, Paths}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt

class ApplicationTest extends AnyFunSuite:

  private val player = PlayerName("Matilde")
  private val onMaze = MapName("test")
  private val start = Command.StartGame(onMaze, player, ModeChoice.Normal)

  /** Rules whose clock runs out before the only collectible of the maze can be reached, so that a
    * game against the clock is lost to the clock rather than won.
    */
  private val RunsOutIn = LevelState.PlayerTimePerPos / 2
  private val soon: ModeTuning = choice =>
    if choice == ModeChoice.Timed then GameMode.Timed(RunsOutIn)
    else summon[ModeTuning].of(choice)
  private val elsewhere = Paths.get("/somewhere/spirale.txt")
  private val aFrameApart = GameSession.LongestStep.toNanos

  /** Frames enough to reach the only collectible, two positions away. Two go by first: one records
    * when it happened, the other starts the crossing.
    */
  private val EnoughToRunOut = (RunsOutIn / GameSession.LongestStep).toInt + 2

  private val EnoughToWin =
    2 * (LevelState.PlayerTimePerPos / GameSession.LongestStep).toInt + 2

  /** The world outside, kept in memory: it answers what it is told to answer and remembers what it
    * was asked to keep.
    */
  private class Outside(
      unreadable: Boolean = false,
      unwritable: Boolean = false,
      resumable: Option[LevelState] = None
  ) extends GameEnvironment:
    val saved: ListBuffer[(LevelState, Played)] = ListBuffer.empty
    val recorded: ListBuffer[(GameResult, MapName)] = ListBuffer.empty
    val kept: ListBuffer[Path] = ListBuffer.empty

    private def refusing[A](answer: A, refused: Boolean): Either[String, A] =
      if refused then Left("the world says no") else Right(answer)

    def mazes: Seq[MapName] = Seq(onMaze)
    def bestOn(maze: MapName): Leaderboard = Leaderboard.empty
    def maze(name: MapName): Either[String, ValidatedMap] =
      refusing(LevelTestSupport.maze, unreadable)
    def mazeAt(path: Path): Either[String, ValidatedMap] =
      refusing(LevelTestSupport.maze, unreadable)
    def keeping(path: Path): Either[String, Unit] = { kept += path; Right(()) }
    def savedGame(path: Path): Either[String, LevelState] =
      resumable.toRight("there is no game to resume")
    def saving(level: LevelState, by: Played): Either[String, Unit] =
      saved += ((level, by))
      refusing((), unwritable)
    def recording(result: GameResult, on: MapName): Either[String, Unit] =
      recorded += ((result, on))
      refusing((), unwritable)

  private def drawingNothing: ValidatedMap => RenderListener[LevelView] = _ => _ => ()

  private def application(
      outside: GameEnvironment = Outside(),
      showing: ValidatedMap => RenderListener[LevelView] = drawingNothing,
      tuning: ModeTuning = ModeTuning.standardModes
  ): Application = Application(outside, showing, tuning)

  /** Whoever draws, together with everything they were shown. */
  private def watching(): (ListBuffer[LevelView], ValidatedMap => RenderListener[LevelView]) =
    val seen = ListBuffer.empty[LevelView]
    (seen, _ => seen.addOne)

  /** The application after a number of frames, each as long as a frame is allowed to be. */
  private def played(from: Application, frames: Int): Application =
    (1 to frames).foldLeft(from)((application, frame) =>
      application.advancedToFrame(frame * aFrameApart)
    )

  test("a game asked for from the menu is played") {
    assert(application().commanded(start).playing.isDefined)
  }

  test("a maze that cannot be read starts no game") {
    assert(application(Outside(unreadable = true)).commanded(start).playing.isEmpty)
  }

  test("a maze that cannot be read is something whoever plays gets told") {
    assert(application(Outside(unreadable = true)).commanded(start).notice.isDefined)
  }

  test("whoever draws is shown the level a game starts on") {
    val (seen, showing) = watching()
    application(showing = showing).commanded(start)
    assert(seen.size == 1)
  }

  test("whoever draws is shown the level again once a frame has changed it") {
    val (seen, showing) = watching()
    played(application(showing = showing).commanded(start), 2)
    assert(seen.size == 2 && seen.head != seen.last)
  }

  test("whoever draws is not shown again a level that a frame left untouched") {
    val (seen, showing) = watching()
    val onHold = application(showing = showing).commanded(start).commanded(Command.Pause)
    played(onHold, 3)
    assert(seen.size == 1)
  }

  test("a game on hold does not advance") {
    val onHold = application().commanded(start).commanded(Command.Pause)
    assert(played(onHold, 3).playing.map(_.session.level) == onHold.playing.map(_.session.level))
  }

  test("a game resumed carries on advancing") {
    val resumed = application()
      .commanded(start)
      .commanded(Command.Pause)
      .commanded(Command.Resume)
    assert(played(resumed, 2).playing.exists(_.session.level.player.isMoving))
  }

  test("a steer is taken while a game is being played") {
    val steered = application().commanded(start).steered(Direction.Down)
    assert(steered.playing.exists(_.session.level.requestedDirection.contains(Direction.Down)))
  }

  test("a steer is refused while a game is on hold") {
    assert(!application().commanded(start).commanded(Command.Pause).steerable)
  }

  test("a steer is refused once a game is over") {
    assert(!played(application().commanded(start), EnoughToWin).steerable)
  }

  test("a steer is refused while no game is being played") {
    assert(!application().steerable)
  }

  test("the score of a game that ends is recorded on the maze it was played on") {
    val outside = Outside()
    played(application(outside).commanded(start), EnoughToWin)
    assert(outside.recorded.map(_._2).toSeq == Seq(onMaze))
  }

  test("a score is recorded once, however many frames follow the end of the game") {
    val outside = Outside()
    played(application(outside).commanded(start), EnoughToWin * 2)
    assert(outside.recorded.size == 1)
  }

  test("a game resumed from a file has no leaderboard to be recorded in") {
    val outside = Outside(resumable = Some(LevelState.from(LevelTestSupport.maze)))
    played(application(outside).commanded(Command.LoadSave(elsewhere, player)), EnoughToWin)
    assert(outside.recorded.isEmpty)
  }

  test("a game is played by the rules that were chosen") {
    val timed = application().commanded(Command.StartGame(onMaze, player, ModeChoice.Timed))
    assert(timed.playing.map(_.session.level.mode).contains(GameMode.Timed(2.minutes)))
  }

  test("a maze read from elsewhere is played by the rules that were chosen too") {
    val survived =
      application().commanded(Command.LoadMap(elsewhere, player, ModeChoice.Survival))
    assert(survived.playing.map(_.session.level.mode).contains(GameMode.Survival()))
  }

  test("a game against the clock is over once its time has run out") {
    val timed =
      application(tuning = soon).commanded(Command.StartGame(onMaze, player, ModeChoice.Timed))
    assert(played(timed, EnoughToRunOut).playing.map(_.status).contains(GameState.Defeat))
  }

  test("a game of survival is not won even with nothing left to collect") {
    val nothingLeft = LevelState
      .from(LevelTestSupport.maze, GameMode.Survival())
      .copy(collectibles = Collectibles(Set.empty))
    val survived = application(Outside(resumable = Some(nothingLeft)))
      .commanded(Command.LoadSave(elsewhere, player))
    assert(survived.playing.map(_.status).contains(GameState.Running))
  }

  test("a game saved on request is put away") {
    assert(application().commanded(start).commanded(Command.SaveAndQuit).playing.isEmpty)
  }

  test("a game saved on request is handed over with who was playing it") {
    val outside = Outside()
    application(outside).commanded(start).commanded(Command.SaveAndQuit)
    assert(outside.saved.map(_._2).toSeq == Seq(Played(player, Some(onMaze))))
  }

  test("a game whose save failed is not put away") {
    val stayed = application(Outside(unwritable = true))
      .commanded(start)
      .commanded(Command.SaveAndQuit)
    assert(stayed.playing.isDefined && stayed.notice.isDefined)
  }

  test("a game left behind is put away") {
    assert(application().commanded(start).commanded(Command.BackToMenu).playing.isEmpty)
  }

  test("a game restarted keeps the rules it was played with") {
    val timed = LevelState.from(LevelTestSupport.maze, GameMode.Timed(30.seconds))
    val again = application(Outside(resumable = Some(timed)))
      .commanded(Command.LoadSave(elsewhere, player))
      .commanded(Command.Restart)
    assert(again.playing.exists(_.session.level.mode == GameMode.Timed(30.seconds)))
  }

  test("a maze read from elsewhere is kept, so that it is offered from then on") {
    val outside = Outside()
    application(outside).commanded(Command.LoadMap(elsewhere, player, ModeChoice.Normal))
    assert(outside.kept.toSeq == Seq(elsewhere))
  }

  test("a maze read from elsewhere is played under the name of its file") {
    val played = application().commanded(Command.LoadMap(elsewhere, player, ModeChoice.Normal))
    assert(played.playing.map(_.by.maze).contains(Some(MapName("spirale"))))
  }

  test("what the application has to say is said once") {
    assert(application(Outside(unreadable = true)).commanded(start).noticed.notice.isEmpty)
  }

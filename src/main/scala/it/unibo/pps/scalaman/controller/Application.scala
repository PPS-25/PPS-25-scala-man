package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.app.{Command, MapName, Played, PlayableMazes, PlayerName}
import it.unibo.pps.scalaman.model.effects.{BonusDuration, Slowdown}
import it.unibo.pps.scalaman.model.map.ValidatedMap
import it.unibo.pps.scalaman.model.score.{GameResult, Leaderboard}
import it.unibo.pps.scalaman.model.{
  Direction,
  GameMode,
  GameState,
  LevelState,
  LoopState,
  ModeTuning
}

import java.nio.file.Path

/** What the application needs of the world outside it. Every answer is either what was asked for or
  * a sentence telling whoever plays why not.
  */
trait GameEnvironment:

  /** Every maze that can be chosen right now. */
  def mazes: Seq[MapName]

  /** A maze the game offers under a name. */
  def maze(name: MapName): Either[String, ValidatedMap]

  /** A maze read from a file of its own. */
  def mazeAt(path: Path): Either[String, ValidatedMap]

  /** Keeps a maze read from elsewhere, so that it is offered from then on. */
  def keeping(path: Path): Either[String, Unit]

  /** A game put away earlier, read back whole. */
  def savedGame(path: Path): Either[String, LevelState]

  /** Puts a game away, under the name of whoever was playing it and where. */
  def saving(level: LevelState, by: Played): Either[String, Unit]

  /** Writes a result among the best scores reached on a maze. */
  def recording(result: GameResult, on: MapName): Either[String, Unit]

  /** The best scores reached on a maze, as they are kept. */
  def bestOn(maze: MapName): Leaderboard

/** A game in progress: what advances it, and who is playing it where. */
final case class Playing(session: GameSession, by: Played):

  /** Whether the game is running, on hold, or done with. */
  def loop: LoopState = session.loop.state

  /** How the level is going. */
  def status: GameState = session.level.status

/** What the application is doing, and what it has to tell whoever plays. Every command lands here.
  * Not free of effects, but free of the frameworks that carry them: hence tested headless.
  */
final case class Application(
    environment: GameEnvironment,
    showing: ValidatedMap => RenderListener[LevelView],
    tuning: ModeTuning,
    playing: Option[Playing] = None,
    notice: Option[String] = None
):

  /** Every maze that can be chosen right now. */
  def mazes: Seq[MapName] = environment.mazes

  /** The best scores reached on a maze. */
  def bestOn(maze: MapName): Leaderboard = environment.bestOn(maze)

  /** The application after whoever plays asked for something. */
  def commanded(command: Command): Application = command match
    case Command.StartGame(maze, player, mode) =>
      begun(environment.maze(maze), Played(player, Some(maze)), tuning.of(mode))
    case Command.LoadMap(path, player, mode) =>
      val read = environment.mazeAt(path)
      // Only a maze that reads is kept, and a maze that cannot be kept is played all the same.
      read.foreach(_ => environment.keeping(path))
      begun(read, Played(player, PlayableMazes.named(path)), tuning.of(mode))
    case Command.LoadSave(path, player) =>
      environment.savedGame(path).fold(told, resumed(_, Played(player, None)))
    case Command.Pause | Command.Resume => onHold
    case Command.Restart                => again
    case Command.BackToMenu             => putAway
    case Command.SaveAndQuit            => putAwayIfSaved

  /** Whether a steer would be taken: not on the menu, not while a game is on hold, and not once it
    * is over.
    */
  def steerable: Boolean =
    playing.exists(current => current.loop == LoopState.Running && !current.session.isOver)

  /** The application after whoever plays asked to turn, which a game that cannot be steered
    * ignores.
    */
  def steered(direction: Direction): Application =
    if !steerable then this
    else advancing(_.requestingDirection(direction))

  /** The application at a frame. A game that ends on this very frame has its score recorded, which
    * is why the world outside is reached from here as well.
    */
  def advancedToFrame(nanos: Long)(using BonusDuration, Slowdown): Application =
    playing.fold(this) { current =>
      val advanced = current.session.advancedToFrame(nanos)
      val ended = !current.session.isOver && advanced.isOver
      val next = copy(playing = Some(current.copy(session = advanced)))
      if ended then next.recorded(advanced.level, current.by) else next
    }

  /** The same application, with what it had to say taken as said. */
  def noticed: Application = copy(notice = None)

  private def begun(
      maze: Either[String, ValidatedMap],
      by: Played,
      mode: GameMode
  ): Application = maze.fold(told, read => resumed(LevelState.from(read, mode), by))

  private def resumed(level: LevelState, by: Played): Application = copy(
    playing = Some(Playing(GameSession.starting(level, showing(level.maze)), by)),
    notice = None
  )

  private def again: Application = playing.fold(this) { current =>
    resumed(
      LevelState.from(current.session.level.maze, current.session.level.mode),
      current.by
    )
  }

  private def putAway: Application = copy(playing = None, notice = None)

  private def putAwayIfSaved: Application = playing.fold(this) { current =>
    environment
      .saving(current.session.level, current.by)
      .fold(told, _ => putAway)
  }

  private def onHold: Application = advancing(_.togglePause)

  private def advancing(step: GameSession => GameSession): Application =
    copy(playing = playing.map(current => current.copy(session = step(current.session))))

  // A game whose maze has no name of its own, which is any game resumed from a file, has no
  // leaderboard to be recorded in.
  private def recorded(level: LevelState, by: Played): Application =
    val written = for
      maze <- by.maze
      result <- level.result(by.player.value)
    yield environment.recording(result, maze)
    written.fold(this)(_.fold(told, _ => this))

  private def told(message: String): Application = copy(notice = Some(message))

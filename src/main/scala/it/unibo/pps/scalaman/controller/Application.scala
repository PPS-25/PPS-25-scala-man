package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.app.{Command, MapName, PlayableMazes, Played, PlayerName}
import it.unibo.pps.scalaman.model.effects.{BonusDuration, Slowdown}
import it.unibo.pps.scalaman.model.map.ValidatedMap
import it.unibo.pps.scalaman.model.score.{GameResult, Leaderboard}
import it.unibo.pps.scalaman.model.{
  Direction,
  GameMode,
  GameState,
  LeaderboardMode,
  LevelState,
  LoopState,
  ModeTuning
}
import it.unibo.pps.scalaman.persistence.SavedGame
import it.unibo.pps.scalaman.view.{LevelView, RenderListener}

import java.nio.file.Path
import java.time.Instant

/** External operations required by the application, with user-facing error messages. */
trait GameEnvironment:

  /** Mazes currently available from the menu. */
  def mazes: Seq[MapName]

  /** Most recently used player name, if available. */
  def playerName: Option[PlayerName]

  /** Directory containing saved games. */
  def savesFolder: Path

  /** Stores the name pre-filled by the menu. */
  def remembering(player: PlayerName): Either[String, Unit]

  /** Loads a maze selected by name. */
  def maze(name: MapName): Either[String, ValidatedMap]

  /** Loads a maze from an arbitrary file. */
  def mazeAt(path: Path): Either[String, ValidatedMap]

  /** Imports an externally selected maze into the user's maps. */
  def keeping(path: Path): Either[String, Unit]

  /** Loads a saved game. */
  def savedGame(path: Path): Either[String, SavedGame]

  /** Saves a level together with its player and source map. */
  def saving(level: LevelState, by: Played): Either[String, Unit]

  /** Records a result in the leaderboard for a map and mode. */
  def recording(result: GameResult, on: MapName, mode: LeaderboardMode): Either[String, Unit]

  /** Reads the leaderboard for a map and mode. */
  def bestOn(maze: MapName, mode: LeaderboardMode): Leaderboard

/** A message the application asks the interface to show. */
enum ApplicationNotice:
  case Error(override val message: String)
  case Information(override val message: String)

  def message: String = this match
    case Error(message)       => message
    case Information(message) => message

/** Running session together with its player and source map. */
final case class Playing(session: GameSession, by: Played):

  /** Current lifecycle state of the game loop. */
  def loop: LoopState = session.loop.state

  /** Current outcome of the level. */
  def status: GameState = session.level.status

  /** Remaining lead-in seconds, if play has not started. */
  def startingIn: Option[Int] = session.countdown

/** Coordinates commands, game state, rendering, and external operations without UI dependencies. */
final case class Application(
    environment: GameEnvironment,
    showing: ValidatedMap => RenderListener[LevelView],
    tuning: ModeTuning,
    playing: Option[Playing] = None,
    notice: Option[ApplicationNotice] = None,
    now: () => Instant = () => Instant.now()
):

  /** Mazes currently available from the menu. */
  def mazes: Seq[MapName] = environment.mazes

  /** Name pre-filled by the menu. */
  def playerName: Option[PlayerName] = environment.playerName

  /** Directory opened by the load-game dialog. */
  def savesFolder: Path = environment.savesFolder

  /** Leaderboard for the selected map and mode. */
  def bestOn(maze: MapName, mode: LeaderboardMode): Leaderboard = environment.bestOn(maze, mode)

  /** Applies a command and returns the resulting application state. */
  def commanded(command: Command): Application = command match
    case Command.StartGame(maze, player, mode) =>
      remembering(player)(
        begun(environment.maze(maze), Played(player, Some(maze)), tuning.of(mode))
      )
    case Command.LoadMap(path, player, mode) =>
      remembering(player) {
        val read = environment.mazeAt(path)
        // Importing is best-effort: a valid map remains playable if copying it fails.
        read.foreach(_ => environment.keeping(path))
        begun(read, Played(player, PlayableMazes.named(path)), tuning.of(mode))
      }
    case Command.LoadSave(path, player) =>
      remembering(player)(
        environment
          .savedGame(path)
          .fold(told, saved => resumed(saved.level, Played(player, saved.maze)))
      )
    case Command.Pause | Command.Resume => onHold
    case Command.Restart                => again
    case Command.BackToMenu             => putAway
    case Command.SaveAndQuit            => putAwayIfSaved

  /** True only while an active, non-terminal game can accept movement input. */
  def steerable: Boolean =
    playing.exists(current => current.loop == LoopState.Running && !current.session.isOver)

  /** Applies a direction request when the current game accepts input. */
  def steered(direction: Direction): Application =
    if !steerable then this
    else advancing(_.requestingDirection(direction))

  /** Advances one frame and records a result when that frame ends the game. */
  def advancedToFrame(nanos: Long)(using BonusDuration, Slowdown): Application =
    playing.fold(this) { current =>
      val advanced = current.session.advancedToFrame(nanos)
      val ended = !current.session.isOver && advanced.isOver
      val next = copy(playing = Some(current.copy(session = advanced)))
      if ended then next.recorded(advanced.level, current.by) else next
    }

  /** Clears the notice after the UI has displayed it. */
  def noticed: Application = copy(notice = None)

  private def begun(
      maze: Either[String, ValidatedMap],
      by: Played,
      mode: GameMode
  ): Application = maze.fold(told, read => resumed(LevelState.from(read, mode), by))

  private def remembering(player: PlayerName)(next: => Application): Application =
    environment.remembering(player).fold(told, _ => next)

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

  // Resumed games have no stable source-map name and therefore no leaderboard entry.
  private def recorded(level: LevelState, by: Played): Application =
    val result = for
      maze <- by.maze
      result <- level.result(by.player.value, now())
    yield (maze, result)
    result.fold(this) { case (maze, score) =>
      environment
        .recording(score, maze, LeaderboardMode.of(level.mode))
        .fold(told, _ => informed(maze, level.mode))
    }

  private def informed(maze: MapName, mode: GameMode): Application = copy(
    notice = Some(
      ApplicationNotice.Information(
        s"Result recorded in the ${LeaderboardMode.of(mode).label} leaderboard for ${maze.value}."
      )
    )
  )

  private def told(message: String): Application =
    copy(notice = Some(ApplicationNotice.Error(message)))

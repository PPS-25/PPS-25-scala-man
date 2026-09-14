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
final case class Playing(session: GameSession, game: Played):

  /** Current lifecycle state of the game loop. */
  def loop: LoopState = session.loop.state

  /** Current outcome of the level. */
  def status: GameState = session.level.status

  /** Remaining lead-in seconds, if play has not started. */
  def startingIn: Option[Int] = session.countdown

/** Coordinates commands, game state, external operations, and an injected level renderer. */
final case class Application(
    environment: GameEnvironment,
    createRenderer: ValidatedMap => RenderListener[LevelView],
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
  def handleCommand(command: Command): Application = command match
    case Command.StartGame(maze, player, mode) =>
      afterRememberingPlayer(player)(
        startWithMaze(environment.maze(maze), Played(player, Some(maze)), tuning.of(mode))
      )
    case Command.LoadMap(path, player, mode) =>
      afterRememberingPlayer(player) {
        val read = environment.mazeAt(path)
        // Importing is best-effort: a valid map remains playable if copying it fails.
        read.foreach(_ => environment.keeping(path))
        startWithMaze(read, Played(player, PlayableMazes.named(path)), tuning.of(mode))
      }
    case Command.LoadSave(path, player) =>
      afterRememberingPlayer(player)(
        environment
          .savedGame(path)
          .fold(errorNotice, saved => startSession(saved.level, Played(player, saved.maze)))
      )
    case Command.Pause | Command.Resume => togglePause
    case Command.Restart                => restart
    case Command.BackToMenu             => returnToMenu
    case Command.SaveAndQuit            => saveAndReturnToMenu

  /** True only while an active, non-terminal game can accept movement input. */
  def acceptsDirectionInput: Boolean =
    playing.exists(current => current.loop == LoopState.Running && !current.session.isOver)

  /** Applies a direction request when the current game accepts input. */
  def requestDirection(direction: Direction): Application =
    if !acceptsDirectionInput then this
    else updateSession(_.requestDirection(direction))

  /** Advances one frame and records a result when that frame ends the game. */
  def advancedToFrame(nanos: Long)(using BonusDuration, Slowdown): Application =
    playing.fold(this) { current =>
      val advanced = current.session.advancedToFrame(nanos)
      val ended = !current.session.isOver && advanced.isOver
      val next = copy(playing = Some(current.copy(session = advanced)))
      if ended then next.recordCompletedGame(advanced.level, current.game) else next
    }

  /** Clears the notice after the UI has displayed it. */
  def noticed: Application = copy(notice = None)

  private def startWithMaze(
      maze: Either[String, ValidatedMap],
      game: Played,
      mode: GameMode
  ): Application = maze.fold(errorNotice, map => startSession(LevelState.from(map, mode), game))

  private def afterRememberingPlayer(player: PlayerName)(next: => Application): Application =
    environment.remembering(player).fold(errorNotice, _ => next)

  private def startSession(level: LevelState, game: Played): Application = copy(
    playing = Some(Playing(GameSession.starting(level, createRenderer(level.maze)), game)),
    notice = None
  )

  private def restart: Application = playing.fold(this) { current =>
    startSession(
      LevelState.from(current.session.level.maze, current.session.level.mode),
      current.game
    )
  }

  private def returnToMenu: Application = copy(playing = None, notice = None)

  private def saveAndReturnToMenu: Application = playing.fold(this) { current =>
    environment
      .saving(current.session.level, current.game)
      .fold(errorNotice, _ => returnToMenu)
  }

  private def togglePause: Application = updateSession(_.togglePause)

  private def updateSession(step: GameSession => GameSession): Application =
    copy(playing = playing.map(current => current.copy(session = step(current.session))))

  // Resumed games have no stable source-map name and therefore no leaderboard entry.
  private def recordCompletedGame(level: LevelState, game: Played): Application =
    val result = for
      maze <- game.maze
      result <- level.result(game.player.value, now())
    yield (maze, result)
    result.fold(this) { case (maze, score) =>
      environment
        .recording(score, maze, LeaderboardMode.of(level.mode))
        .fold(errorNotice, _ => resultRecordedNotice(maze, level.mode))
    }

  private def resultRecordedNotice(maze: MapName, mode: GameMode): Application = copy(
    notice = Some(
      ApplicationNotice.Information(
        s"Result recorded in the ${LeaderboardMode.of(mode).label} leaderboard for ${maze.value}."
      )
    )
  )

  private def errorNotice(message: String): Application =
    copy(notice = Some(ApplicationNotice.Error(message)))

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
import it.unibo.pps.scalaman.presentation.{LevelView, RenderListener}

import java.nio.file.Path
import java.time.Instant

trait GameEnvironment:

  def mazes: Seq[MapName]

  def playerName: Option[PlayerName]

  def savesFolder: Path

  def remembering(player: PlayerName): Either[String, Unit]

  def maze(name: MapName): Either[String, ValidatedMap]

  def mazeAt(path: Path): Either[String, ValidatedMap]

  def keeping(path: Path): Either[String, Unit]

  def savedGame(path: Path): Either[String, SavedGame]

  def saving(level: LevelState, by: Played): Either[String, Unit]

  def recording(result: GameResult, on: MapName, mode: LeaderboardMode): Either[String, Unit]

  def bestOn(maze: MapName, mode: LeaderboardMode): Leaderboard

enum ApplicationNotice:
  case Error(override val message: String)
  case Information(override val message: String)

  def message: String = this match
    case Error(message)       => message
    case Information(message) => message

final case class Playing(session: GameSession, by: Played):

  def loop: LoopState = session.loop.state

  def status: GameState = session.level.status

  def startingIn: Option[Int] = session.countdown

final case class Application(
    environment: GameEnvironment,
    showing: ValidatedMap => RenderListener[LevelView],
    tuning: ModeTuning,
    playing: Option[Playing] = None,
    notice: Option[ApplicationNotice] = None,
    now: () => Instant = () => Instant.now()
):

  def mazes: Seq[MapName] = environment.mazes

  def playerName: Option[PlayerName] = environment.playerName

  def savesFolder: Path = environment.savesFolder

  def bestOn(maze: MapName, mode: LeaderboardMode): Leaderboard = environment.bestOn(maze, mode)

  def commanded(command: Command): Application = command match
    case Command.StartGame(maze, player, mode) =>
      remembering(player)(
        begun(environment.maze(maze), Played(player, Some(maze)), tuning.of(mode))
      )
    case Command.LoadMap(path, player, mode) =>
      remembering(player) {
        val read = environment.mazeAt(path)
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

  def steerable: Boolean =
    playing.exists(current => current.loop == LoopState.Running && !current.session.isOver)

  def steered(direction: Direction): Application =
    if !steerable then this
    else advancing(_.requestingDirection(direction))

  def advancedToFrame(nanos: Long)(using BonusDuration, Slowdown): Application =
    playing.fold(this) { current =>
      val advanced = current.session.advancedToFrame(nanos)
      val ended = !current.session.isOver && advanced.isOver
      val next = copy(playing = Some(current.copy(session = advanced)))
      if ended then next.recorded(advanced.level, current.by) else next
    }

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

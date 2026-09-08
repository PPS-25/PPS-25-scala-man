package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.app.{
  DefaultMaps,
  GameFiles,
  MapName,
  Played,
  PlayableMazes,
  PlayerName
}
import it.unibo.pps.scalaman.leaderboard.io.FileLeaderboardStorage
import it.unibo.pps.scalaman.map.io.MapLoader
import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.{LeaderboardMode, LevelState}
import it.unibo.pps.scalaman.model.map.{
  MapLoadError,
  MapParseError,
  MapValidationError,
  ValidatedMap
}
import it.unibo.pps.scalaman.model.score.{GameResult, Leaderboard, LeaderboardError}
import it.unibo.pps.scalaman.persistence.{GameSaveRepository, SaveGameError}

import java.io.IOException
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Using

/** The world outside as it really is, over the files of whoever plays. Every error becomes a
  * sentence, because whoever reads it is playing a game rather than debugging one.
  */
final class GameFilesEnvironment(files: GameFiles, saves: GameSaveRepository)
    extends GameEnvironment:

  import GameFilesEnvironment.*

  def mazes: Seq[MapName] = PlayableMazes.offered(found(files.mazes))

  def playerName: Option[PlayerName] =
    Option
      .when(Files.isRegularFile(files.playerName))(files.playerName)
      .flatMap(path => scala.util.Try(Files.readString(path).trim).toOption)
      .flatMap(name => Option.when(name.nonEmpty)(PlayerName(name)))

  def remembering(player: PlayerName): Either[String, Unit] =
    if playerName.contains(player) then Right(())
    else
      for
        _ <- made(files.home)
        _ <- attempted("the player name cannot be kept")(
          Files.writeString(files.playerName, player.value)
        )
      yield ()

  def maze(name: MapName): Either[String, ValidatedMap] =
    PlayableMazes.fileOf(name, files.mazes).fold(shipped(name))(mazeAt)

  def mazeAt(path: Path): Either[String, ValidatedMap] =
    MapLoader.load(path).left.map(described).flatMap(read)

  // Nothing is ever written over, and a name the game already ships with is not taken.
  def keeping(path: Path): Either[String, Unit] =
    PlayableMazes
      .named(path)
      .flatMap(name => PlayableMazes.fileOf(name, files.mazes))
      .filterNot(Files.exists(_))
      .fold(Right(()))(copying(path))

  def savedGame(path: Path): Either[String, LevelState] = saves.load(path).left.map(described)

  def saving(level: LevelState, by: Played): Either[String, Unit] =
    for
      folder <- made(files.saves)
      _ <- saves.save(level, folder.resolve(fileFor(by))).left.map(described)
    yield ()

  def recording(result: GameResult, on: MapName, mode: LeaderboardMode): Either[String, Unit] =
    val storage = FileLeaderboardStorage(files.leaderboardOf(on, mode))
    // A recording reads the result out of a state: here the state handed to it is the result.
    LeaderboardRecording[GameResult](Some.apply, storage).recording(result).left.map(described)

  // Best scores nobody can read are shown as none reached: a menu has nowhere to tell it.
  def bestOn(maze: MapName, mode: LeaderboardMode): Leaderboard =
    FileLeaderboardStorage(files.leaderboardOf(maze, mode)).load().getOrElse(Leaderboard.empty)

  private def shipped(name: MapName): Either[String, ValidatedMap] =
    DefaultMaps
      .textOf(name)
      .toRight(s"the maze ${name.value} is missing from the application")
      .flatMap(read)

  private def read(text: String): Either[String, ValidatedMap] =
    for
      raw <- MapParser.parse(text).left.map(_.map(spelled).mkString("; "))
      maze <- MapValidator.validate(raw).left.map(_.map(spelled).mkString("; "))
    yield maze

  private def copying(from: Path)(to: Path): Either[String, Unit] =
    made(files.mazes).flatMap(_ =>
      attempted(s"the maze cannot be kept for next time")(Files.copy(from, to))
    )

  private def made(folder: Path): Either[String, Path] =
    attempted(s"the folder $folder cannot be made")(Files.createDirectories(folder))

  private def found(folder: Path): Seq[Path] =
    if !Files.isDirectory(folder) then Seq.empty
    else Using(Files.list(folder))(_.iterator.asScala.toSeq).getOrElse(Seq.empty)

object GameFilesEnvironment:

  /** The world of whoever is running the game. */
  def ofUser(saves: GameSaveRepository): GameFilesEnvironment =
    GameFilesEnvironment(GameFiles.ofUser, saves)

  private val Unnamed = "game"

  /** What a saved game is called: the maze and who was playing it, kept to what a file name can
    * hold, so that saving the same game again writes over it instead of piling up.
    */
  private def fileFor(by: Played): String =
    s"${by.maze.map(_.value).getOrElse(Unnamed)}-${plainly(by.player.value)}.properties"

  private def plainly(name: String): String =
    name.map(letter => if letter.isLetterOrDigit then letter else '-')

  private def attempted[A](whenRefused: String)(action: => A): Either[String, A] =
    try Right(action)
    catch case error: IOException => Left(s"$whenRefused: ${error.getMessage}")

  private def described(error: MapLoadError): String = error match
    case MapLoadError.FileNotFound(path)       => s"there is no file at $path"
    case MapLoadError.ReadFailed(path, reason) => s"$path cannot be read: $reason"

  private def described(error: SaveGameError): String = error match
    case SaveGameError.FileNotFound(path)        => s"there is no saved game at $path"
    case SaveGameError.ReadFailed(path, reason)  => s"the game at $path cannot be read: $reason"
    case SaveGameError.WriteFailed(path, reason) => s"the game cannot be kept at $path: $reason"
    case SaveGameError.InvalidSave(reason)       => s"that saved game cannot be used: $reason"

  private def described(error: LeaderboardError): String = error match
    case LeaderboardError.ReadFailed(path, reason) =>
      s"the best scores at $path cannot be read: $reason"
    case LeaderboardError.WriteFailed(path, reason) =>
      s"the best scores cannot be written to $path: $reason"
    case LeaderboardError.Malformed(line) => s"a line of the best scores cannot be read: $line"

  private def spelled(error: MapParseError): String = error match
    case MapParseError.EmptyMap                         => "the maze is empty"
    case MapParseError.RaggedRow(row, expected, actual) =>
      s"line ${row + 1} holds $actual positions instead of $expected"
    case MapParseError.UnsupportedSymbol(symbol, row, col) =>
      s"'$symbol' at line ${row + 1}, column ${col + 1} is not something a maze can hold"

  private def spelled(error: MapValidationError): String = error match
    case MapValidationError.InvalidDimensions(height, width) =>
      s"a maze of $height by $width is too small to be played"
    case MapValidationError.MissingSpawn => "the maze has nowhere for the player to start"
    case MapValidationError.InvalidSpawnCount(count) =>
      s"the maze has $count places for the player to start instead of one"
    case MapValidationError.MissingCollectible         => "the maze has nothing to collect"
    case MapValidationError.MissingEnemy               => "the maze has no enemy"
    case MapValidationError.UnreachableCollectible(at) =>
      s"the collectible at line ${at.row + 1}, column ${at.col + 1} cannot be reached"
    case MapValidationError.UnreachableEnemy(at) =>
      s"the enemy at line ${at.row + 1}, column ${at.col + 1} cannot be reached"
    case MapValidationError.InvalidTeleportPair(code, ends) =>
      s"teleport $code needs one end of each kind, and $ends were found"
    case MapValidationError.UnsupportedTeleportCode(code) =>
      s"teleport $code is not one the game knows"
    case MapValidationError.OpenBorder(positions) =>
      s"the maze is open at ${positions.size} places along its border"

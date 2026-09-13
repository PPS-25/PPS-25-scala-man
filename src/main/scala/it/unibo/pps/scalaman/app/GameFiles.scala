package it.unibo.pps.scalaman.app

import it.unibo.pps.scalaman.model.LeaderboardMode

import java.nio.file.{Path, Paths}
import scala.io.Source
import scala.util.Using

/** Locations used to persist data between application runs. */
final case class GameFiles(home: Path):

  /** Directory containing saved games. */
  def saves: Path = home.resolve(GameFiles.Saves)

  /** Directory containing imported maps. */
  def mazes: Path = home.resolve(GameFiles.Mazes)

  /** File containing the last player name. */
  def playerName: Path = home.resolve(GameFiles.PlayerName)

  /** Path of a map's leaderboard; classic mode preserves the legacy filename. */
  def leaderboardOf(map: MapName, mode: LeaderboardMode): Path =
    val suffix = mode match
      case LeaderboardMode.Classic  => ""
      case LeaderboardMode.Timed    => "-timed"
      case LeaderboardMode.Survival => "-survival"
    home.resolve(GameFiles.Leaderboards).resolve(s"${map.value}$suffix.csv")

  /** Path of a map's classic leaderboard. */
  def leaderboardOf(map: MapName): Path = leaderboardOf(map, LeaderboardMode.Classic)

object GameFiles:

  private val Folder = ".scala-man"
  private val Saves = "saves"
  private val Mazes = "maps"
  private val PlayerName = "player-name.txt"
  private val Leaderboards = "leaderboards"

  /** Default data directory for the current user. */
  def ofUser: GameFiles = GameFiles(Paths.get(System.getProperty("user.home"), Folder))

/** Maps bundled with the application. */
object DefaultMaps:

  /** Bundled maps available from the menu. */
  val All: Seq[MapName] = Seq(
    "easy-hunter",
    "easy-anticipator",
    "easy-patroller",
    "medium",
    "hard"
  ).map(MapName.apply)

  /** Classpath resource containing a bundled map. */
  def resourceOf(map: MapName): String = s"/maps/${map.value}.txt"

  /** Text of a bundled map, when its resource is available. */
  def textOf(map: MapName): Option[String] =
    Option(getClass.getResourceAsStream(resourceOf(map)))
      .flatMap(stream => Using(Source.fromInputStream(stream))(_.mkString).toOption)

/** Naming and lookup rules for bundled and imported maps. */
object PlayableMazes:

  private val Extension = ".txt"

  /** Derives a valid map name from a filename without its final extension. */
  def named(path: Path): Option[MapName] =
    Option(path.getFileName)
      .map(file => withoutExtension(file.toString))
      .flatMap(MapName.from)

  /** Lists bundled maps first and excludes imported maps that would share their leaderboard. */
  def offered(found: Seq[Path]): Seq[MapName] =
    DefaultMaps.All ++ found
      .filter(_.getFileName.toString.endsWith(Extension))
      .flatMap(named)
      .distinct
      .filterNot(DefaultMaps.All.contains)

  /** File for an imported map; bundled maps have no user file. */
  def fileOf(maze: MapName, folder: Path): Option[Path] =
    Option.when(!DefaultMaps.All.contains(maze))(folder.resolve(s"${maze.value}$Extension"))

  private def withoutExtension(file: String): String = file.lastIndexOf('.') match
    case -1  => file
    case dot => file.take(dot)

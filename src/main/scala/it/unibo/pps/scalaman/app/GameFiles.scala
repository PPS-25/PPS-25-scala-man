package it.unibo.pps.scalaman.app

import it.unibo.pps.scalaman.model.LeaderboardMode

import java.nio.file.{Path, Paths}
import scala.io.Source
import scala.util.Using

final case class GameFiles(home: Path):
  def saves: Path = home.resolve(GameFiles.Saves)
  def mazes: Path = home.resolve(GameFiles.Mazes)
  def playerName: Path = home.resolve(GameFiles.PlayerName)
  def leaderboardOf(map: MapName, mode: LeaderboardMode): Path =
    val suffix = mode match
      case LeaderboardMode.Classic  => ""
      case LeaderboardMode.Timed    => "-timed"
      case LeaderboardMode.Survival => "-survival"
    home.resolve(GameFiles.Leaderboards).resolve(s"${map.value}$suffix.csv")

  def leaderboardOf(map: MapName): Path = leaderboardOf(map, LeaderboardMode.Classic)

object GameFiles:

  private val Folder = ".scala-man"
  private val Saves = "saves"
  private val Mazes = "maps"
  private val PlayerName = "player-name.txt"
  private val Leaderboards = "leaderboards"

  def ofUser: GameFiles = GameFiles(Paths.get(System.getProperty("user.home"), Folder))

object DefaultMaps:
  val All: Seq[MapName] = Seq(
    "easy-hunter",
    "easy-anticipator",
    "easy-patroller",
    "medium",
    "hard"
  ).map(MapName.apply)

  def resourceOf(map: MapName): String = s"/maps/${map.value}.txt"
  def textOf(map: MapName): Option[String] =
    Option(getClass.getResourceAsStream(resourceOf(map)))
      .flatMap(stream => Using(Source.fromInputStream(stream))(_.mkString).toOption)

object PlayableMazes:

  private val Extension = ".txt"

  def named(path: Path): Option[MapName] =
    Option(path.getFileName)
      .map(file => withoutExtension(file.toString))
      .filter(_.nonEmpty)
      .map(MapName.apply)

  def offered(found: Seq[Path]): Seq[MapName] =
    DefaultMaps.All ++ found
      .filter(_.getFileName.toString.endsWith(Extension))
      .flatMap(named)
      .distinct
      .filterNot(DefaultMaps.All.contains)

  def fileOf(maze: MapName, folder: Path): Option[Path] =
    Option.when(!DefaultMaps.All.contains(maze))(folder.resolve(s"${maze.value}$Extension"))

  private def withoutExtension(file: String): String = file.lastIndexOf('.') match
    case -1  => file
    case dot => file.take(dot)

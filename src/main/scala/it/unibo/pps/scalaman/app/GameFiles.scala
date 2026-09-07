package it.unibo.pps.scalaman.app

import java.nio.file.{Path, Paths}
import scala.io.Source
import scala.util.Using

/** Where the game keeps what it remembers between one run and the next. */
final case class GameFiles(home: Path):

  /** Where the saved games are kept. */
  def saves: Path = home.resolve(GameFiles.Saves)

  /** Where whoever plays keeps the mazes they added themselves. */
  def mazes: Path = home.resolve(GameFiles.Mazes)

  /** Where the best scores reached on a maze are kept, one file per maze. */
  def leaderboardOf(map: MapName): Path =
    home.resolve(GameFiles.Leaderboards).resolve(s"${map.value}.csv")

object GameFiles:

  private val Folder = ".scala-man"
  private val Saves = "saves"
  private val Mazes = "maps"
  private val Leaderboards = "leaderboards"

  /** Where the game keeps its files for whoever is running it. */
  def ofUser: GameFiles = GameFiles(Paths.get(System.getProperty("user.home"), Folder))

/** The mazes the game is shipped with. */
object DefaultMaps:

  /** Every maze that can be chosen without looking for a file. */
  val All: Seq[MapName] = Seq("classic", "crossroads", "arena").map(MapName.apply)

  /** The file a shipped maze is read from. */
  def resourceOf(map: MapName): String = s"/maps/${map.value}.txt"

  /** What a shipped maze is drawn as, or nothing if it cannot be read. */
  def textOf(map: MapName): Option[String] =
    Option(getClass.getResourceAsStream(resourceOf(map)))
      .flatMap(stream => Using(Source.fromInputStream(stream))(_.mkString).toOption)

/** Which mazes can be chosen, and where each of them is read from. */
object PlayableMazes:

  private val Extension = ".txt"

  /** The name a maze file is known by: its own, with the extension dropped. A file left with
    * nothing once the extension is gone names no maze.
    */
  def named(path: Path): Option[MapName] =
    Option(path.getFileName)
      .map(file => withoutExtension(file.toString))
      .filter(_.nonEmpty)
      .map(MapName.apply)

  /** Every maze that can be chosen, shipped ones first. A name is what tells one leaderboard from
    * another, so a maze named after a shipped one is left out rather than sharing it.
    */
  def offered(found: Seq[Path]): Seq[MapName] =
    DefaultMaps.All ++ found
      .filter(_.getFileName.toString.endsWith(Extension))
      .flatMap(named)
      .distinct
      .filterNot(DefaultMaps.All.contains)

  /** The file a maze is read from, or nothing when the game ships with it. */
  def fileOf(maze: MapName, folder: Path): Option[Path] =
    Option.when(!DefaultMaps.All.contains(maze))(folder.resolve(s"${maze.value}$Extension"))

  private def withoutExtension(file: String): String = file.lastIndexOf('.') match
    case -1  => file
    case dot => file.take(dot)

package it.unibo.pps.scalaman.app

import java.nio.file.{Path, Paths}
import scala.io.Source
import scala.util.Using

/** Where the game keeps what it remembers between one run and the next. */
final case class GameFiles(home: Path):

  /** Where the saved games are kept. */
  def saves: Path = home.resolve(GameFiles.Saves)

  /** Where the best scores reached on a maze are kept, one file per maze. */
  def leaderboardOf(map: MapName): Path =
    home.resolve(GameFiles.Leaderboards).resolve(s"${map.value}.csv")

object GameFiles:

  private val Folder = ".scala-man"
  private val Saves = "saves"
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

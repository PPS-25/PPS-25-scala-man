package it.unibo.pps.scalaman.app

import java.nio.file.Path

/** A maze the game can be played on, named after the file it is read from. The name is also how its
  * leaderboard is told apart from the others.
  */
final case class MapName(value: String):
  require(value.nonEmpty, "a map must have a name")

/** Who is playing, as the leaderboard will remember them. */
final case class PlayerName(value: String):
  require(value.trim.nonEmpty, "a player must have a name")

/** Who is playing and on which maze, which a level does not know about itself. A game resumed from
  * a file has no maze name, so no leaderboard of its own.
  */
final case class Played(player: PlayerName, maze: Option[MapName])

/** Something the player asks for, which the interface offers but never carries out itself. */
enum Command:
  case StartGame(map: MapName, player: PlayerName)
  case LoadMap(path: Path, player: PlayerName)
  case LoadSave(path: Path, player: PlayerName)
  case Pause, Restart, Resume, SaveAndQuit, BackToMenu

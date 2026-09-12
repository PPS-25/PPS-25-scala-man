package it.unibo.pps.scalaman.app

import it.unibo.pps.scalaman.model.ModeChoice

import java.nio.file.Path

final case class MapName(value: String):
  require(MapName.isSafe(value), "a map name must be a safe file name")

object MapName:
  def from(value: String): Option[MapName] =
    Option.when(isSafe(value))(MapName(value))

  private def isSafe(value: String): Boolean =
    value.nonEmpty &&
      value != "." &&
      value != ".." &&
      value.forall(char => char.isLetterOrDigit || ".-_ ".contains(char))

final case class PlayerName(value: String):
  require(value.trim.nonEmpty, "a player must have a name")

final case class Played(player: PlayerName, maze: Option[MapName])

enum Command:
  case StartGame(map: MapName, player: PlayerName, mode: ModeChoice)
  case LoadMap(path: Path, player: PlayerName, mode: ModeChoice)
  case LoadSave(path: Path, player: PlayerName)
  case Pause, Restart, Resume, SaveAndQuit, BackToMenu

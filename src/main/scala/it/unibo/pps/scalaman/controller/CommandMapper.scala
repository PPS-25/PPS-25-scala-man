package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.Direction

object CommandMapper:

  /** Converts a key press into a movement command.
    */
  def toDir(key: String): Option[Direction] = key.toUpperCase match
    case "UP" | "W"    => Some(Direction.Up)
    case "DOWN" | "S"  => Some(Direction.Down)
    case "LEFT" | "A"  => Some(Direction.Left)
    case "RIGHT" | "D" => Some(Direction.Right)
    case _             => None

  /** Whether this key press should pause or resume the game */
  def isPauseKey(key: String): Boolean = key.equalsIgnoreCase("ESCAPE")

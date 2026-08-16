package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.Direction
import scalafx.scene.input.KeyCode

object CommandMapper:

  /** Converts a key press into a movement command.
    */
  def toDir(key: KeyCode): Option[Direction] = key match
    case KeyCode.Up | KeyCode.W    => Some(Direction.Up)
    case KeyCode.Down | KeyCode.S  => Some(Direction.Down)
    case KeyCode.Left | KeyCode.A  => Some(Direction.Left)
    case KeyCode.Right | KeyCode.D => Some(Direction.Right)
    case _                         => None

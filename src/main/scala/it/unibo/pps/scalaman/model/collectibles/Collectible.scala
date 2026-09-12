package it.unibo.pps.scalaman.model.collectibles

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.effects.BonusEffect

sealed trait Collectible:
  def position: Position

object Collectible:

  final case class Basic(position: Position) extends Collectible

  final case class Bonus(position: Position, effect: BonusEffect) extends Collectible

package it.unibo.pps.scalaman.model.collectibles

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.effects.BonusEffect

/** An element placed on a position of the map that the player can pick up by reaching it.
  */
sealed trait Collectible:
  /** The position the collectible is placed on. */
  def position: Position

object Collectible:

  /** A standard collectible: picking up every one of them completes the level.
    * @param position
    *   the position the collectible is placed on.
    */
  final case class Basic(position: Position) extends Collectible

  /** A collectible granting a temporary effect, irrelevant to level completion.
    * @param position
    *   the position the collectible is placed on.
    * @param effect
    *   the effect granted when the bonus is collected.
    */
  final case class Bonus(position: Position, effect: BonusEffect) extends Collectible

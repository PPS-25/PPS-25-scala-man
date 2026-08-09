package it.unibo.pps.scalaman.model

/** An element placed on a cell of the map that the player can pick up by
  * reaching it.
  */
sealed trait Collectible:
  /** The cell the collectible is placed on. */
  def cell: Cell

object Collectible:

  /** A standard collectible: picking up every one of them completes the level.
    * @param cell
    *   the cell the collectible is placed on.
    */
  final case class Basic(cell: Cell) extends Collectible

  /** A collectible granting a temporary effect, irrelevant to level completion.
    * @param cell
    *   the cell the collectible is placed on.
    * @param effect
    *   the effect granted when the bonus is collected.
    */
  final case class Bonus(cell: Cell, effect: BonusEffect) extends Collectible

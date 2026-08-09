package it.unibo.pps.scalaman.model

/** The outcome of picking up on a cell.
  * @param left
  *   the collectibles still on the map afterwards.
  * @param element
  *   the element picked up, if the cell held one.
  */
final case class Collected(left: Collectibles, element: Option[Collectible])

/** The collectibles still present on the map, indexed by the cell they occupy.
  * Collecting is idempotent: a cell that has already been emptied yields
  * nothing.
  */
trait Collectibles:

  /** The collectible placed on a cell, if any. */
  def at(cell: Cell): Option[Collectible]

  /** How many standard collectibles are still to be picked up. */
  def remaining: Int

  /** Whether no standard collectible is left, that is, the level is over. */
  def isLevelComplete: Boolean = remaining == 0

  /** Picks up the collectible placed on a cell.
    * @param cell
    *   the cell reached by the player.
    */
  def collect(cell: Cell): Collected

object Collectibles:

  /** Places collectibles on the map, ignoring duplicates on the same cell. */
  def apply(elements: Iterable[Collectible]): Collectibles =
    OnCells(elements.map(element => element.cell -> element).toMap)

  private final case class OnCells(elements: Map[Cell, Collectible])
      extends Collectibles:

    def at(cell: Cell): Option[Collectible] = elements.get(cell)

    def remaining: Int = elements.values.count:
      case _: Collectible.Basic => true
      case _: Collectible.Bonus => false

    def collect(cell: Cell): Collected = at(cell) match
      case Some(element) => Collected(OnCells(elements - cell), Some(element))
      case None          => Collected(this, None)

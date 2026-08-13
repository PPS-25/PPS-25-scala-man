package it.unibo.pps.scalaman.model

/** The outcome of picking up on a position.
  * @param left
  *   the collectibles still on the map afterwards.
  * @param element
  *   the element picked up, if the position held one.
  */
final case class Collected(left: Collectibles, element: Option[Collectible])

/** The collectibles still present on the map, indexed by the position they occupy. Collecting is
  * idempotent: a position that has already been emptied yields nothing.
  */
trait Collectibles:

  /** The collectible placed on a position, if any. */
  def at(position: Position): Option[Collectible]

  /** How many standard collectibles are still to be picked up. */
  def remaining: Int

  /** Whether no standard collectible is left, that is, the level is over. */
  def isLevelComplete: Boolean = remaining == 0

  /** Picks up the collectible placed on a position.
    * @param position
    *   the position reached by the player.
    */
  def collect(position: Position): Collected

object Collectibles:

  /** Places collectibles on the map, ignoring duplicates on the same position. */
  def apply(elements: Iterable[Collectible]): Collectibles =
    OnPositions(elements.map(element => element.position -> element).toMap)

  private final case class OnPositions(elements: Map[Position, Collectible]) extends Collectibles:

    def at(position: Position): Option[Collectible] = elements.get(position)

    def remaining: Int = elements.values.count:
      case _: Collectible.Basic => true
      case _: Collectible.Bonus => false

    def collect(position: Position): Collected = at(position) match
      case Some(element) => Collected(OnPositions(elements - position), Some(element))
      case None          => Collected(this, None)

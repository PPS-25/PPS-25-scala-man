package it.unibo.pps.scalaman.model.collectibles

import it.unibo.pps.scalaman.model.Position

final case class Collected(left: Collectibles, element: Option[Collectible])

trait Collectibles:
  def at(position: Position): Option[Collectible]
  def placed: Set[Collectible]
  def remaining: Int
  def isLevelComplete: Boolean = remaining == 0
  def collect(position: Position): Collected

object Collectibles:

  def apply(elements: Iterable[Collectible]): Collectibles =
    OnPositions(elements.map(element => element.position -> element).toMap)

  private final case class OnPositions(elements: Map[Position, Collectible]) extends Collectibles:

    def at(position: Position): Option[Collectible] = elements.get(position)

    def placed: Set[Collectible] = elements.values.toSet

    def remaining: Int = elements.values.count:
      case _: Collectible.Basic => true
      case _: Collectible.Bonus => false

    def collect(position: Position): Collected = at(position) match
      case Some(element) => Collected(OnPositions(elements - position), Some(element))
      case None          => Collected(this, None)

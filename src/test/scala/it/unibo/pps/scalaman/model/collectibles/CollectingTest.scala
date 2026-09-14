package it.unibo.pps.scalaman.model.collectibles

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.collectibles.Collectible.Basic
import it.unibo.pps.scalaman.model.Direction.Right
import it.unibo.pps.scalaman.model.entities.MovingEntity
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class CollectingTest extends AnyFunSuite:
  private val timePerPos = 100.millis
  private val emptyPosition = Position(0, 0)
  private val item = Basic(emptyPosition + Right)

  private val collectibles = Collectibles(Set(item))
  private val standingPlayer = MovingEntity(item.position, Right, timePerPos)
  private val approachingPlayer =
    MovingEntity(emptyPosition, Right, timePerPos).move(Right, _ => true)

  test("a player standing on a standard collectible collects it") {
    assert(collectibles.collectedBy(standingPlayer).element.contains(item))
  }

  test(
    "a player collecting a standard collectible decreases the remaining ones"
  ) {
    assert(
      collectibles.collectedBy(standingPlayer).left.remaining ==
        collectibles.remaining - 1
    )
  }

  test("a player standing on a position holding nothing collects nothing") {
    val playerOnEmptyPosition = MovingEntity(emptyPosition, Right, timePerPos)
    assert(collectibles.collectedBy(playerOnEmptyPosition).element.isEmpty)
  }

  test("a player collects nothing while still moving towards the collectible") {
    assert(collectibles.collectedBy(approachingPlayer).element.isEmpty)
  }

  test(
    "a player collects the element once its movement towards it is complete"
  ) {
    val arrivedPlayer = approachingPlayer.update(timePerPos)
    assert(collectibles.collectedBy(arrivedPlayer).element.contains(item))
  }

  test("a player standing again on an emptied position picks up nothing") {
    val afterCollection = collectibles.collectedBy(standingPlayer).left
    val again = afterCollection.collectedBy(standingPlayer)
    assert(again.element.isEmpty)
    assert(again.left.remaining == afterCollection.remaining)
  }

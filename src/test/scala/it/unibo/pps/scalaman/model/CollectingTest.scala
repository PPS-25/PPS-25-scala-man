package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Collectible.Basic
import it.unibo.pps.scalaman.model.Direction.Right
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class CollectingTest extends AnyFunSuite:
  private val timePerCell = 100.millis
  private val emptyCell = Cell(0, 0)
  private val item = Basic(emptyCell + Right)

  private val collectibles = Collectibles(Set(item))
  private val standingPlayer = MovingEntity(item.cell, Right, timePerCell)
  private val approachingPlayer =
    MovingEntity(emptyCell, Right, timePerCell).move(Right)

  test("a player standing on a standard collectible collects it") {
    assert(collectibles.collectedBy(standingPlayer).element.contains(item))
  }

  test("what a player collects is no longer on the map") {
    assert(collectibles.collectedBy(standingPlayer).left.at(item.cell).isEmpty)
  }

  test(
    "a player collecting a standard collectible decreases the remaining ones"
  ) {
    assert(
      collectibles.collectedBy(standingPlayer).left.remaining ==
        collectibles.remaining - 1
    )
  }

  test("a player standing on a cell holding nothing collects nothing") {
    val playerOnEmptyCell = MovingEntity(emptyCell, Right, timePerCell)
    assert(collectibles.collectedBy(playerOnEmptyCell).element.isEmpty)
  }

  test("a player collects nothing while still moving towards the collectible") {
    assert(collectibles.collectedBy(approachingPlayer).element.isEmpty)
  }

  test(
    "a player collects the element once its movement towards it is complete"
  ) {
    val arrivedPlayer = approachingPlayer.update(timePerCell)
    assert(collectibles.collectedBy(arrivedPlayer).element.contains(item))
  }

  test("a player standing again on an emptied cell collects nothing") {
    val afterCollection = collectibles.collectedBy(standingPlayer).left
    assert(afterCollection.collectedBy(standingPlayer).element.isEmpty)
  }

  test(
    "a player standing again on an emptied cell leaves the remaining ones untouched"
  ) {
    val afterCollection = collectibles.collectedBy(standingPlayer).left
    assert(
      afterCollection.collectedBy(standingPlayer).left.remaining ==
        afterCollection.remaining
    )
  }

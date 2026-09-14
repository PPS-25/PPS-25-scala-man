package it.unibo.pps.scalaman.model.collectibles

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.effects.BonusEffect.Invulnerability
import it.unibo.pps.scalaman.model.collectibles.Collectible.{Basic, Bonus}
import org.scalatest.funsuite.AnyFunSuite

class CollectiblesTest extends AnyFunSuite:
  private val basic = Basic(Position(1, 1))
  private val anotherBasic = Basic(Position(2, 1))
  private val bonus = Bonus(Position(3, 1), Invulnerability)
  private val emptyPosition = Position(9, 9)

  private val collectibles = Collectibles(Set(basic, anotherBasic, bonus))

  test("collecting on a position holding nothing leaves everything as it was") {
    val collected = collectibles.collect(emptyPosition)
    assert(collected.element.isEmpty)
    assert(collected.left.remaining == collectibles.remaining)
  }

  test(
    "collecting a standard collectible yields the element placed on that position"
  ) {
    assert(collectibles.collect(basic.position).element.contains(basic))
  }

  test("a collected element is no longer on the map") {
    val left = collectibles.collect(basic.position).left
    assert(left.at(basic.position).isEmpty)
    assert(!left.placed.contains(basic))
  }

  test(
    "the remaining standard collectibles are as many as the ones placed on the map"
  ) {
    assert(collectibles.remaining == 2)
  }

  test(
    "collecting a standard collectible decreases the remaining ones by one"
  ) {
    assert(
      collectibles
        .collect(basic.position)
        .left
        .remaining == collectibles.remaining - 1
    )
  }

  test("collecting twice on the same position picks up nothing the second time") {
    val afterFirstCollection = collectibles.collect(basic.position).left
    val afterSecond = afterFirstCollection.collect(basic.position)
    assert(afterSecond.element.isEmpty)
    assert(afterSecond.left.remaining == collectibles.remaining - 1)
  }

  test(
    "collecting a bonus does not change the remaining standard collectibles"
  ) {
    assert(
      collectibles.collect(bonus.position).left.remaining == collectibles.remaining
    )
  }

  test(
    "a level is complete when no standard collectible is left, even if a bonus was not"
  ) {
    val emptied = Seq(basic, anotherBasic)
      .foldLeft(collectibles)((left, element) => left.collect(element.position).left)
    assert(emptied.isLevelComplete)
  }

  test("what is placed on the map is what was put there") {
    assert(collectibles.placed == Set(basic, anotherBasic, bonus))
  }

  test("a map holding no standard collectible is complete from the start") {
    assert(Collectibles(Set(bonus)).isLevelComplete)
  }

  test("a level is not complete while a standard collectible is left") {
    assert(!collectibles.collect(basic.position).left.isLevelComplete)
  }

package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.BonusEffect.Invulnerability
import it.unibo.pps.scalaman.model.Collectible.{Basic, Bonus}
import org.scalatest.funsuite.AnyFunSuite

class CollectiblesTest extends AnyFunSuite:
  private val basic = Basic(Position(1, 1))
  private val anotherBasic = Basic(Position(2, 1))
  private val bonus = Bonus(Position(3, 1), Invulnerability)
  private val emptyPosition = Position(9, 9)

  private val collectibles = Collectibles(Set(basic, anotherBasic, bonus))

  test("collecting on a position holding nothing yields no element") {
    assert(collectibles.collect(emptyPosition).element.isEmpty)
  }

  test(
    "collecting on a position holding nothing leaves the collectibles unchanged"
  ) {
    assert(
      collectibles.collect(emptyPosition).left.remaining == collectibles.remaining
    )
  }

  test(
    "collecting a standard collectible yields the element placed on that position"
  ) {
    assert(collectibles.collect(basic.position).element.contains(basic))
  }

  test("a collected element is no longer on the map") {
    assert(collectibles.collect(basic.position).left.at(basic.position).isEmpty)
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

  test("collecting twice on the same position yields nothing the second time") {
    val afterFirstCollection = collectibles.collect(basic.position).left
    assert(afterFirstCollection.collect(basic.position).element.isEmpty)
  }

  test(
    "collecting twice on the same position decreases the remaining ones only once"
  ) {
    val afterFirstCollection = collectibles.collect(basic.position).left
    assert(
      afterFirstCollection
        .collect(basic.position)
        .left
        .remaining == collectibles.remaining - 1
    )
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

  test("a map holding no standard collectible is complete from the start") {
    assert(Collectibles(Set(bonus)).isLevelComplete)
  }

  test("a level is not complete while a standard collectible is left") {
    assert(!collectibles.collect(basic.position).left.isLevelComplete)
  }

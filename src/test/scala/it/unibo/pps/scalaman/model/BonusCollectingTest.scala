package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.BonusEffect.{Invulnerability, SlowDown}
import it.unibo.pps.scalaman.model.Collectible.{Basic, Bonus}
import it.unibo.pps.scalaman.model.Direction.Right
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class BonusCollectingTest extends AnyFunSuite:
  private val timePerCell = 100.millis
  private val now = 0.seconds
  private val lasting = summon[BonusDuration].of(Invulnerability)

  private val bonus = Bonus(Cell(1, 0), Invulnerability)
  private val basic = Basic(Cell(2, 0))
  private val collectibles = Collectibles(Set(bonus, basic))
  private val noEffect = ActiveEffects.empty

  private def playerOn(cell: Cell) = MovingEntity(cell, Right, timePerCell)
  private def collectedOn(cell: Cell) =
    collectibles.collectedBy(playerOn(cell)).element

  test("a collected bonus is no longer on the map") {
    val left = collectibles.collectedBy(playerOn(bonus.cell)).left
    assert(left.at(bonus.cell).isEmpty)
  }

  test("collecting a bonus activates the effect it carries") {
    assert(
      noEffect
        .grantedBy(collectedOn(bonus.cell), now)
        .isActive(Invulnerability, now)
    )
  }

  test("collecting a bonus activates no effect other than the one it carries") {
    assert(
      !noEffect.grantedBy(collectedOn(bonus.cell), now).isActive(SlowDown, now)
    )
  }

  test("collecting a standard collectible activates no effect") {
    assert(noEffect.grantedBy(collectedOn(basic.cell), now).active(now).isEmpty)
  }

  test("collecting nothing leaves the active effects untouched") {
    assert(noEffect.grantedBy(None, now).active(now).isEmpty)
  }

  test(
    "the effect granted by a bonus is applied until its configured duration"
  ) {
    val granted = noEffect.grantedBy(collectedOn(bonus.cell), now)
    assert(granted.isActive(Invulnerability, now + lasting - 1.milli))
  }

  test(
    "the effect granted by a bonus expires once its configured duration is over"
  ) {
    val granted = noEffect.grantedBy(collectedOn(bonus.cell), now)
    assert(!granted.isActive(Invulnerability, now + lasting))
  }

  test(
    "standing again on the cell of a collected bonus grants no further effect"
  ) {
    val Collected(left, taken) = collectibles.collectedBy(playerOn(bonus.cell))
    val granted = noEffect.grantedBy(taken, now)
    val later = now + lasting / 2
    val regranted =
      granted.grantedBy(left.collectedBy(playerOn(bonus.cell)).element, later)
    assert(!regranted.isActive(Invulnerability, now + lasting))
  }

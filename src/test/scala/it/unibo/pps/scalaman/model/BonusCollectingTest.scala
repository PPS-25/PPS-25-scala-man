package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.BonusEffect.{Invulnerability, SlowDown}
import it.unibo.pps.scalaman.model.Collectible.{Basic, Bonus}
import it.unibo.pps.scalaman.model.Direction.Right
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class BonusCollectingTest extends AnyFunSuite:
  private val timePerPos = 100.millis
  private val now = 0.seconds
  private val lasting = summon[BonusDuration].of(Invulnerability)

  private val bonus = Bonus(Position(1, 0), Invulnerability)
  private val basic = Basic(Position(2, 0))
  private val collectibles = Collectibles(Set(bonus, basic))
  private val noEffect = ActiveEffects.empty

  private def playerOn(position: Position) = MovingEntity(position, Right, timePerPos)
  private def collectedOn(position: Position) =
    collectibles.collectedBy(playerOn(position)).element

  test("a collected bonus is no longer on the map") {
    val left = collectibles.collectedBy(playerOn(bonus.position)).left
    assert(left.at(bonus.position).isEmpty)
  }

  test("collecting a bonus activates the effect it carries") {
    assert(
      noEffect
        .grantedBy(collectedOn(bonus.position), now)
        .isActive(Invulnerability, now)
    )
  }

  test("collecting a bonus activates no effect other than the one it carries") {
    assert(
      !noEffect.grantedBy(collectedOn(bonus.position), now).isActive(SlowDown, now)
    )
  }

  test("collecting a standard collectible activates no effect") {
    assert(noEffect.grantedBy(collectedOn(basic.position), now).active(now).isEmpty)
  }

  test("collecting nothing leaves the active effects untouched") {
    assert(noEffect.grantedBy(None, now).active(now).isEmpty)
  }

  test(
    "the effect granted by a bonus is applied until its configured duration"
  ) {
    val granted = noEffect.grantedBy(collectedOn(bonus.position), now)
    assert(granted.isActive(Invulnerability, now + lasting - 1.milli))
  }

  test(
    "the effect granted by a bonus expires once its configured duration is over"
  ) {
    val granted = noEffect.grantedBy(collectedOn(bonus.position), now)
    assert(!granted.isActive(Invulnerability, now + lasting))
  }

  test(
    "standing again on the position of a collected bonus grants no further effect"
  ) {
    val Collected(left, taken) = collectibles.collectedBy(playerOn(bonus.position))
    val granted = noEffect.grantedBy(taken, now)
    val later = now + lasting / 2
    val regranted =
      granted.grantedBy(left.collectedBy(playerOn(bonus.position)).element, later)
    assert(!regranted.isActive(Invulnerability, now + lasting))
  }

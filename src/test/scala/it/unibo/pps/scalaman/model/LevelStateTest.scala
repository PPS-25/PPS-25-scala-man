package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.effects.BonusEffect.{Invulnerability, SlowDown}
import it.unibo.pps.scalaman.model.effects.ActiveEffects
import it.unibo.pps.scalaman.model.LevelTestSupport.{
  bonus,
  item,
  lasting,
  levelWith,
  startingLevel,
  timePerPos
}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class LevelStateTest extends AnyFunSuite:

  test("a tick moves the level clock forward") {
    assert(startingLevel.ticking(timePerPos).clock.elapsed == timePerPos)
  }

  test("standing on nothing collects nothing") {
    assert(startingLevel.collecting.collectibles.remaining == 1)
  }

  test("standing on a standard collectible picks it up") {
    assert(levelWith(item.position).collecting.collectibles.isLevelComplete)
  }

  test("standing on a bonus applies the effect it carries") {
    val collected = levelWith(bonus.position).collecting
    assert(collected.effects.isActive(Invulnerability, collected.clock.elapsed))
  }

  test("an effect is dropped once the level ticked past its expiration") {
    val protectedLevel = levelWith(bonus.position).collecting
    val later = protectedLevel.ticking(lasting).withoutExpiredEffects
    assert(later.effects == ActiveEffects.empty)
  }

  test("enemies wait longer between steps while the level holds the slow down") {
    val betweenSteps = 200.millis
    val slowed = startingLevel.copy(effects =
      ActiveEffects.empty.activate(SlowDown, startingLevel.clock.elapsed, lasting)
    )
    assert(slowed.enemyStepInterval(betweenSteps) == betweenSteps * 2)
  }

  test("an effect is kept while the level has not ticked past its expiration") {
    val protectedLevel = levelWith(bonus.position).collecting
    val soon = protectedLevel.ticking(lasting / 2).withoutExpiredEffects
    assert(soon.effects.isActive(Invulnerability, soon.clock.elapsed))
  }

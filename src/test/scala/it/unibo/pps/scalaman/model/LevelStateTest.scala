package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.effects.BonusEffect.Invulnerability
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

  test("an effect is kept while the level has not ticked past its expiration") {
    val protectedLevel = levelWith(bonus.position).collecting
    val soon = protectedLevel.ticking(lasting / 2).withoutExpiredEffects
    assert(soon.effects.isActive(Invulnerability, soon.clock.elapsed))
  }

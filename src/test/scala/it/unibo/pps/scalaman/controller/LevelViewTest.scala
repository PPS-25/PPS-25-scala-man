package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.effects.BonusEffect.Invulnerability
import it.unibo.pps.scalaman.model.LevelTestSupport.{bonus, item, levelWith, startingLevel}
import it.unibo.pps.scalaman.model.LevelState
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ListBuffer

class LevelViewTest extends AnyFunSuite:

  private def recorder(): (ListBuffer[LevelView], RenderListener[LevelView]) =
    val recorded = ListBuffer.empty[LevelView]
    (recorded, recorded.addOne)

  test("the view is shown where the player is") {
    assert(LevelView.of(startingLevel).player == startingLevel.player.currentPos)
  }

  test("the view is shown what is still on the map, bonuses included") {
    assert(LevelView.of(startingLevel).collectibles == Set(item, bonus))
  }

  test("the view is shown where the enemies are") {
    assert(LevelView.of(startingLevel).enemies == startingLevel.enemies)
  }

  test("the view is shown how much is left to pick up") {
    assert(LevelView.of(startingLevel).remaining == 1)
  }

  test("the view is shown how the level is going") {
    assert(LevelView.of(startingLevel).status == startingLevel.status)
  }

  test("the view is shown the lives left") {
    assert(LevelView.of(startingLevel).lives == 3)
  }

  test("the view is shown which effects are applied") {
    val protectedLevel = levelWith(bonus.position).collecting
    assert(LevelView.of(protectedLevel).applied == Set(Invulnerability))
  }

  test("a tick that picks something up is shown to the view") {
    val (recorded, listener) = recorder()
    val rendering = LevelView.rendering.subscribing(listener)
    LevelState.pipeline().tickNotifying(levelWith(item.position), rendering)
    assert(recorded.toSeq.map(_.remaining) == Seq(0))
  }

  test("a tick that picks nothing up is not shown to the view again") {
    val (recorded, listener) = recorder()
    val rendering = LevelView.rendering.subscribing(listener).notifying(startingLevel)
    recorded.clear()
    LevelState.pipeline().tickNotifying(startingLevel, rendering)
    assert(recorded.isEmpty)
  }

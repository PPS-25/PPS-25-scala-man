package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.effects.BonusEffect.Invulnerability
import it.unibo.pps.scalaman.model.LevelTestSupport.{
  bonus,
  item,
  levelWith,
  startingLevel,
  timePerPos
}
import it.unibo.pps.scalaman.model.{Direction, LevelState}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt

class LevelViewTest extends AnyFunSuite:

  private def recorder(): (ListBuffer[LevelView], RenderListener[LevelView]) =
    val recorded = ListBuffer.empty[LevelView]
    (recorded, recorded.addOne)

  test("the view is shown where the player is") {
    assert(LevelView.of(startingLevel).player.from == startingLevel.player.currentPos)
  }

  test("the view is shown what is still on the map, bonuses included") {
    assert(LevelView.of(startingLevel).collectibles == Set(item, bonus))
  }

  test("the view is shown where the enemies are") {
    assert(
      LevelView.of(startingLevel).enemies.map(_.at.from) ==
        startingLevel.enemies.map(_.currentPos)
    )
  }

  test("the view is shown how far along a crossing something is") {
    val moving = startingLevel.movingPlayer(_.move(Direction.Right, _ => true))
    val halfWay = moving.movingPlayer(_.update(timePerPos / 2))
    assert(LevelView.of(halfWay).player.progress == 0.5)
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
    LevelState.pipeline(timePerPos).tickNotifying(levelWith(item.position), rendering)
    assert(recorded.toSeq.map(_.remaining) == Seq(0))
  }

  test("a tick that picks nothing up is not shown to the view again") {
    val (recorded, listener) = recorder()
    val rendering = LevelView.rendering.subscribing(listener).notifying(startingLevel)
    recorded.clear()
    LevelState.pipeline(timePerPos).tickNotifying(startingLevel, rendering)
    assert(recorded.isEmpty)
  }

  test("the view is shown the score so far") {
    assert(LevelView.of(startingLevel).score == startingLevel.score.currentScore)
  }

  test("the view is shown the time in the whole seconds it is going to show") {
    val played = startingLevel.ticking(1500.millis)
    assert(LevelView.of(played).elapsed == 1.second)
  }

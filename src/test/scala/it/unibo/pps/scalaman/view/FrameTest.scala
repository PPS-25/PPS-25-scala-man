package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.controller.LevelView
import it.unibo.pps.scalaman.model.LevelTestSupport.{
  bonus,
  item,
  levelWith,
  startingLevel,
  timePerPos
}
import it.unibo.pps.scalaman.model.effects.BonusEffect.Invulnerability
import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.{Direction, GameState, LevelState, Position}
import org.scalatest.funsuite.AnyFunSuite

class FrameTest extends AnyFunSuite:

  private def frameOf(level: LevelState): Frame = Frame.of(LevelView.of(level))

  private def drawnOn(frame: Frame, position: Position): Set[Sprite] =
    frame.entities.filter(_.at == Spot.on(position)).map(_.sprite).toSet

  private val hunterSpawn = Position(3, 1)
  private val frame = frameOf(startingLevel)

  test("the player is drawn where it stands") {
    assert(drawnOn(frame, startingLevel.player.currentPos).contains(Sprite.Player(Mouth.Open)))
  }

  test("an enemy is drawn as the kind it is") {
    assert(drawnOn(frame, hunterSpawn).contains(Sprite.Enemy(EnemyKind.Hunter)))
  }

  test("the mouth of the player opens and closes as it steps") {
    val stepped = Position(0, 1)
    assert(drawnOn(frameOf(levelWith(stepped)), stepped).contains(Sprite.Player(Mouth.Closed)))
  }

  test("someone crossing between two cells is drawn between them") {
    val moving = startingLevel.movingPlayer(_.move(Direction.Right, _ => true))
    val halfWay = moving.movingPlayer(_.update(timePerPos / 2))
    assert(frameOf(halfWay).entities.last.at == Spot(0.0, 0.5))
  }

  test("what is left to pick up is drawn") {
    assert(drawnOn(frame, item.position).contains(Sprite.Item))
  }

  test("a bonus is drawn as the effect it grants") {
    assert(drawnOn(frame, bonus.position).contains(Sprite.Bonus(Invulnerability)))
  }

  test("the player is drawn last, so that it covers everyone") {
    assert(frameOf(levelWith(hunterSpawn)).entities.last.sprite == Sprite.Player(Mouth.Open))
  }

  test("what has been picked up is drawn no more") {
    val picked = frameOf(levelWith(item.position).collecting)
    assert(!picked.entities.map(_.sprite).contains(Sprite.Item))
  }

  test("nothing is drawn where nothing stands") {
    assert(drawnOn(frame, Position(2, 3)).isEmpty)
  }

  test("the status bar shows the lives left") {
    assert(frame.status.lives == 3)
  }

  test("the status bar shows what is left to pick up") {
    assert(frame.status.remaining == 1)
  }

  test("the status bar shows the effects applied") {
    assert(frameOf(levelWith(bonus.position).collecting).status.applied == Set(Invulnerability))
  }

  test("the status bar shows how the level is going") {
    assert(frame.status.state == GameState.Running)
  }

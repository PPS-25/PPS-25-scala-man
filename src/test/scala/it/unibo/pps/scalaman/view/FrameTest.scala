package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.controller.LevelView
import it.unibo.pps.scalaman.model.LevelTestSupport.{bonus, item, levelWith, startingLevel}
import it.unibo.pps.scalaman.model.effects.BonusEffect.Invulnerability
import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.{GameState, LevelState, Position}
import org.scalatest.funsuite.AnyFunSuite

class FrameTest extends AnyFunSuite:

  private def frameOf(level: LevelState): Frame = Frame.of(LevelView.of(level))

  private def isPlayer(sprite: Sprite): Boolean = sprite match
    case Sprite.Player(_) => true
    case _                => false

  private val hunterSpawn = Position(3, 1)
  private val frame = frameOf(startingLevel)

  test("the player is drawn where it stands") {
    assert(frame.at(startingLevel.player.currentPos).contains(Sprite.Player(Mouth.Open)))
  }

  test("an enemy is drawn as the kind it is") {
    assert(frame.at(hunterSpawn).contains(Sprite.Enemy(EnemyKind.Hunter)))
  }

  test("the mouth of the player opens and closes as it steps") {
    val stepped = Position(0, 1)
    assert(frameOf(levelWith(stepped)).at(stepped).contains(Sprite.Player(Mouth.Closed)))
  }

  test("what is left to pick up is drawn") {
    assert(frame.at(item.position).contains(Sprite.Item))
  }

  test("a bonus is drawn as the effect it grants") {
    assert(frame.at(bonus.position).contains(Sprite.Bonus(Invulnerability)))
  }

  test("the player covers an enemy standing on the same position") {
    assert(frameOf(levelWith(hunterSpawn)).at(hunterSpawn).exists(isPlayer))
  }

  test("what has been picked up is drawn no more") {
    assert(
      !frameOf(levelWith(item.position).collecting).entities.values.toSet.contains(Sprite.Item)
    )
  }

  test("nothing is drawn where nothing stands") {
    assert(frame.at(Position(2, 3)).isEmpty)
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

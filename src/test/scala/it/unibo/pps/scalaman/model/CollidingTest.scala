package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.effects.BonusEffect.{Invulnerability, SlowDown}
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusDuration}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class CollidingTest extends AnyFunSuite:
  private val now = 0.seconds
  private val lasting = summon[BonusDuration].of(Invulnerability)

  private val progress = LevelProgress.initial
  private val defenceless = ActiveEffects.empty
  private val invulnerable = defenceless.activate(Invulnerability, now, lasting)

  test("meeting an enemy costs a life") {
    assert(progress.afterCollision(defenceless, now).lives == progress.lives - 1)
  }

  test("meeting an enemy while invulnerable costs nothing") {
    assert(progress.afterCollision(invulnerable, now).lives == progress.lives)
  }

  test("meeting an enemy once invulnerability expired costs a life") {
    val expired = now + lasting
    assert(progress.afterCollision(invulnerable, expired).lives == progress.lives - 1)
  }

  test("meeting several enemies during one invulnerability costs nothing") {
    val unharmed = progress
      .afterCollision(invulnerable, now)
      .afterCollision(invulnerable, now + lasting / 2)
    assert(unharmed.lives == progress.lives)
  }

  test("the slow down does not protect from meeting an enemy") {
    val slowed = defenceless.activate(SlowDown, now, lasting)
    assert(progress.afterCollision(slowed, now).lives == progress.lives - 1)
  }

  test("meeting an enemy with no life left leaves the level over") {
    assert(LevelProgress(1).afterCollision(defenceless, now).isOver)
  }

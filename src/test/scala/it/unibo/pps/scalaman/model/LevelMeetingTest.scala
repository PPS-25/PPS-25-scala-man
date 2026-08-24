package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Direction.Right
import it.unibo.pps.scalaman.model.LevelTestSupport.{lasting, levelWith}
import it.unibo.pps.scalaman.model.effects.ActiveEffects
import it.unibo.pps.scalaman.model.effects.BonusEffect.{Invulnerability, SlowDown}
import it.unibo.pps.scalaman.model.entities.{Enemy, MovingEntity}
import it.unibo.pps.scalaman.model.map.EnemyKind
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class LevelMeetingTest extends AnyFunSuite:
  private val where = Position(1, 1)
  private val nextTo = Position(1, 2)

  private def enemyAt(position: Position) =
    Enemy(MovingEntity(position, Right, 250.millis), EnemyKind.Hunter)

  private def levelWithEnemies(playerAt: Position, enemies: Enemy*) =
    levelWith(playerAt).copy(enemies = enemies.toVector)

  test("an enemy standing on the player is a meeting") {
    assert(levelWithEnemies(where, enemyAt(where)).metAnEnemy)
  }

  test("an enemy standing next to the player is no meeting") {
    assert(!levelWithEnemies(where, enemyAt(nextTo)).metAnEnemy)
  }

  test("a level with no enemy left has no meeting") {
    assert(!levelWithEnemies(where).metAnEnemy)
  }

  test("only one enemy out of many has to stand on the player") {
    assert(levelWithEnemies(where, enemyAt(nextTo), enemyAt(where)).metAnEnemy)
  }

  private val met = levelWithEnemies(where, enemyAt(where))
  private val protectedFromIt =
    met.copy(effects = ActiveEffects.empty.activate(Invulnerability, met.clock.elapsed, lasting))

  test("meeting an enemy costs a life") {
    assert(met.afterMeetingEnemies.progress.lives == met.progress.lives - 1)
  }

  test("meeting an enemy sends the player back to its spawn") {
    assert(met.afterMeetingEnemies.player.currentPos == met.maze.spawn)
  }

  test("meeting an enemy sends the enemies back to their spawns") {
    assert(
      met.afterMeetingEnemies.enemies.map(_.currentPos).toSet == met.maze.enemies.map(_.position)
    )
  }

  test("meeting an enemy interrupts the movement the player was making") {
    val onItsWay = met.movingPlayer(_.move(Right, _ => true))
    assert(onItsWay.afterMeetingEnemies.player.movement.isEmpty)
  }

  test("meeting no enemy leaves the level alone") {
    val untouched = levelWithEnemies(where, enemyAt(nextTo))
    assert(untouched.afterMeetingEnemies == untouched)
  }

  test("meeting an enemy while invulnerable costs no life") {
    assert(protectedFromIt.afterMeetingEnemies.progress == protectedFromIt.progress)
  }

  test("meeting an enemy while invulnerable sends nobody back") {
    assert(protectedFromIt.afterMeetingEnemies.player.currentPos == where)
  }

  test("going back to the spawn keeps what was already picked up") {
    assert(met.afterMeetingEnemies.collectibles.remaining == met.collectibles.remaining)
  }

  test("going back to the spawn keeps the effects that are still applied") {
    val slowed =
      met.copy(effects = ActiveEffects.empty.activate(SlowDown, met.clock.elapsed, lasting))
    assert(slowed.afterMeetingEnemies.effects == slowed.effects)
  }

  test("going back to the spawn forgets where the player came from") {
    val walkedIntoOne = levelWithEnemies(where, enemyAt(nextTo))
      .movingPlayer(_.move(Right, _ => true).update(LevelState.PlayerTimePerPos))
    assert(walkedIntoOne.afterMeetingEnemies.playerPreviousPos.isEmpty)
  }

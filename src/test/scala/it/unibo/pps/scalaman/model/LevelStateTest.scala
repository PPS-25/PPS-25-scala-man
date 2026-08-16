package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Direction.Right
import it.unibo.pps.scalaman.model.LevelTestSupport.{
  bonus,
  item,
  lasting,
  levelWith,
  maze,
  startingLevel,
  timePerPos
}
import it.unibo.pps.scalaman.model.collectibles.Collectible.{Basic, Bonus}
import it.unibo.pps.scalaman.model.effects.ActiveEffects
import it.unibo.pps.scalaman.model.effects.BonusEffect.{Invulnerability, SlowDown}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class LevelStateTest extends AnyFunSuite:
  private val fromMaze = LevelState.from(maze)
  private val between = LevelState.BetweenEnemySteps

  test("a level starts with the player on the spawn of the maze") {
    assert(fromMaze.player.currentPos == maze.spawn)
  }

  test("a level starts with the enemies the maze holds") {
    assert(fromMaze.enemies.toSet == maze.enemies)
  }

  test("a level starts with the standard collectibles the maze holds") {
    assert(fromMaze.collectibles.placed.contains(Basic(Position(1, 3))))
  }

  test("a level reads the bonuses the maze holds") {
    assert(fromMaze.collectibles.placed.contains(Bonus(Position(1, 5), Invulnerability)))
  }

  test("a level counts as remaining only the standard collectibles of the maze") {
    assert(fromMaze.collectibles.remaining == maze.collectibles.size)
  }

  test("a level starts with no effect applied") {
    assert(fromMaze.effects.active(fromMaze.clock.elapsed).isEmpty)
  }

  test("a level starts with the lives the game gives") {
    assert(fromMaze.progress == LevelProgress.initial)
  }

  test("a tick runs the stage the enemies are moved by") {
    val movedAway = LevelState.pipeline(updateAi = _.enemiesStepped(Vector.empty))
    assert(movedAway.tick(startingLevel).enemies.isEmpty)
  }

  test("a tick leaves the enemies alone when no stage moves them") {
    assert(LevelState.pipeline().tick(startingLevel).enemies == startingLevel.enemies)
  }

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

  test("the player leaves no previous position behind while standing still") {
    assert(startingLevel.movingPlayer(identity).playerPreviousPos.isEmpty)
  }

  test("the player leaves its previous position behind once it changes place") {
    val moved = startingLevel.movingPlayer(_.move(Right, _ => true).update(timePerPos))
    assert(moved.playerPreviousPos.contains(startingLevel.player.currentPos))
  }

  test("the player leaves no previous position behind while still on its way") {
    val moving = startingLevel.movingPlayer(_.move(Right, _ => true))
    assert(moving.playerPreviousPos.isEmpty)
  }

  test("enemies do not step before their interval has passed") {
    assert(!startingLevel.ticking(between - 1.milli).enemyStepDue)
  }

  test("enemies step once their interval has passed") {
    assert(startingLevel.ticking(between).enemyStepDue)
  }

  test("enemies wait twice as long while the slow down is applied") {
    val slowed = startingLevel.copy(effects =
      ActiveEffects.empty.activate(SlowDown, startingLevel.clock.elapsed, lasting)
    )
    assert(!slowed.ticking(between).enemyStepDue)
  }

  test("a stepped enemy starts waiting again") {
    val due = startingLevel.ticking(between)
    assert(!due.enemiesStepped(due.enemies).enemyStepDue)
  }

  test("stepping the enemies puts them where they moved") {
    val moved = startingLevel.enemies.map(enemy => enemy.copy(position = Position(1, 1)))
    assert(startingLevel.enemiesStepped(moved).enemies == moved)
  }

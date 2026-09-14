package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.GameState.{Defeat, Running, Victory}
import it.unibo.pps.scalaman.model.LevelTestSupport.{item, levelWith, startingLevel, timePerPos}
import it.unibo.pps.scalaman.model.collectibles.Collectibles
import it.unibo.pps.scalaman.model.entities.{Enemy, MovingEntity}
import it.unibo.pps.scalaman.model.map.EnemyKind
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class LevelOutcomeTest extends AnyFunSuite:
  private val nothingLeft = startingLevel.copy(collectibles = Collectibles(Set.empty))
  private val outOfLives = startingLevel.copy(progress = LevelProgress(0))
  private val won = nothingLeft
  private val lost = outOfLives

  test("a level with something left to pick up is running") {
    assert(startingLevel.status == Running)
  }

  test("a level with nothing left to pick up is won") {
    assert(nothingLeft.status == Victory)
  }

  test("a level with no life left is lost") {
    assert(outOfLives.status == Defeat)
  }

  test("a level that ran out of lives on the last collectible is lost") {
    assert(nothingLeft.copy(progress = LevelProgress(0)).status == Defeat)
  }

  test("time stands still once the level ended") {
    assert(won.ticking(1.second) == won)
  }

  test("time keeps running while the level does") {
    assert(startingLevel.ticking(1.second) != startingLevel)
  }

  test("a tick picks nothing up once the level ended") {
    val onACollectible = levelWith(item.position).copy(progress = LevelProgress(0))
    assert(LevelState.pipeline(timePerPos).tick(onACollectible) == onACollectible)
  }

  test("a tick does not move the enemies once the level ended") {
    val elsewhere =
      Vector(Enemy(MovingEntity(Position(2, 2), Direction.Right, 250.millis), EnemyKind.Hunter))
    val moving = LevelState.pipeline(timePerPos, updateAi = _.enemiesStepped(elsewhere))
    assert(moving.tick(lost) == lost)
  }

  test("a tick still moves the enemies while the level runs") {
    val elsewhere =
      Vector(Enemy(MovingEntity(Position(2, 2), Direction.Right, 250.millis), EnemyKind.Hunter))
    val moving = LevelState.pipeline(timePerPos, updateAi = _.enemiesStepped(elsewhere))
    assert(moving.tick(startingLevel).enemies == elsewhere)
  }

package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.GameState.{Defeat, Running, Victory}
import it.unibo.pps.scalaman.model.LevelTestSupport.{maze, startingLevel}
import it.unibo.pps.scalaman.model.collectibles.Collectibles
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusEffect}
import it.unibo.pps.scalaman.model.entities.{Enemy, MovingEntity}
import it.unibo.pps.scalaman.model.map.EnemyKind
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class GameModeSpec extends AnyFunSuite:

  private def movingEnemy: Enemy =
    Enemy(
      MovingEntity(Position(3, 1), Direction.Right, LevelState.EnemyTimePerPos)
        .move(Direction.Right, _ => true),
      EnemyKind.Hunter
    )

  test("normal mode preserves the standard victory and defeat rules") {
    val won = startingLevel.copy(collectibles = Collectibles(Set.empty))
    val lost = startingLevel.copy(progress = LevelProgress(0))

    assert(won.status == Victory)
    assert(lost.status == Defeat)
  }

  test("a timed mode remains running before its limit") {
    val timed = LevelState.from(maze, GameMode.Timed(1.second)).ticking(999.millis)

    assert(timed.status == Running)
  }

  test("a timed mode loses when its limit expires") {
    val timed = LevelState.from(maze, GameMode.Timed(1.second)).ticking(1.second)

    assert(timed.status == Defeat)
  }

  test("a timed mode loses at the limit even when the last collectible is gone") {
    val timed = LevelState
      .from(maze, GameMode.Timed(1.second))
      .copy(collectibles = Collectibles(Set.empty), clock = GameClock(1.second))

    assert(timed.status == Defeat)
  }

  test("a timed mode requires a positive time limit") {
    assertThrows[IllegalArgumentException] {
      GameMode.Timed(0.seconds)
    }
  }

  test("a survival mode stays running after every collectible is collected") {
    val survival = LevelState
      .from(maze, GameMode.Survival())
      .copy(collectibles = Collectibles(Set.empty))

    assert(survival.status == Running)
  }

  test("a survival mode tracks elapsed survival time and ends when lives run out") {
    val survival = LevelState.from(maze, GameMode.Survival()).ticking(5.seconds)

    assert(survival.clock.elapsed == 5.seconds)
    assert(survival.copy(progress = LevelProgress(0)).status == Defeat)
  }

  test("a survival mode progressively speeds enemy movement up to its configured maximum") {
    val mode = GameMode.Survival(difficultyEvery = 1.second, maximumSpeedMultiplier = 5)
    val accelerated =
      LevelState.from(maze, mode).ticking(1.second).copy(enemies = Vector(movingEnemy))
    val capped = LevelState.from(maze, mode).ticking(4.seconds).copy(enemies = Vector(movingEnemy))

    assert(!accelerated.movingOn(125.millis).enemies.head.entity.isMoving)
    assert(!capped.movingOn(50.millis).enemies.head.entity.isMoving)
  }

  test("slowdown is applied after the survival speed increase") {
    val mode = GameMode.Survival(difficultyEvery = 1.second, maximumSpeedMultiplier = 5)
    val advanced = LevelState
      .from(maze, mode)
      .ticking(1.second)
      .copy(enemies = Vector(movingEnemy))
    val slowed = advanced.copy(effects =
      ActiveEffects.empty.activate(BonusEffect.SlowDown, advanced.clock.elapsed, 1.second)
    )

    assert(slowed.movingOn(125.millis).enemies.head.entity.isMoving)
    assert(!slowed.movingOn(250.millis).enemies.head.entity.isMoving)
  }

  test("a survival mode requires positive difficulty tuning") {
    assertThrows[IllegalArgumentException] {
      GameMode.Survival(difficultyEvery = 0.seconds)
    }
    assertThrows[IllegalArgumentException] {
      GameMode.Survival(maximumSpeedMultiplier = 0)
    }
  }

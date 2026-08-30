package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Direction.Right
import it.unibo.pps.scalaman.model.LevelTestSupport.levelWith
import it.unibo.pps.scalaman.model.{LevelState, Position}
import it.unibo.pps.scalaman.model.entities.Enemy
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class EnemyAiStageSpec extends AnyFunSuite:
  private val level = levelWith(Position(1, 1))

  test("the standard AI stage starts a movement for every idle enemy") {
    val updated = EnemyAiStage.stage(level)

    assert(updated.enemies.forall(_.entity.isMoving))
    assert(updated.enemies.head.entity.movement.exists(_.to == Position(2, 1)))
  }

  test("the standard level pipeline runs the AI stage by default") {
    val updated = LevelState.pipeline(100.millis).tick(level)

    assert(updated.enemies.forall(_.entity.isMoving))
  }

  test("the AI stage does not replace an enemy movement in progress") {
    val inProgress = level.enemies.head.moving(_.move(Right, level.maze.isWalkable))
    val updated = EnemyAiStage.stage(level.copy(enemies = Vector(inProgress)))

    assert(updated.enemies == Vector(inProgress))
  }

  test("the AI stage accepts a replacement strategy selection") {
    val alwaysRight = new EnemyMovementStrategy:
      def nextMove(context: EnemyMovementContext): Option[Position] =
        Some(context.enemyPosition + Right)
    val hunter = level.enemies.head

    val updated = EnemyAiStage.stage(
      level.copy(enemies = Vector(hunter)),
      EnemyStrategySelection(_ => alwaysRight)
    )

    assert(updated.enemies.head.entity.movement.exists(_.to == hunter.currentPos + Right))
  }

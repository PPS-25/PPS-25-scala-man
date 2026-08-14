package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.RawMap
import it.unibo.pps.scalaman.model.map.Tile
import it.unibo.pps.scalaman.model.map.ValidatedMap
import org.scalatest.funsuite.AnyFunSuite

class EnemyMovementStrategySpec extends AnyFunSuite:
  private val openMap = validatedMap(
    Vector(
      Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
      Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
      Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
      Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
      Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
    )
  )

  private val mapWithBlockedCorridor = validatedMap(
    Vector(
      Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
      Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
      Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Wall, Tile.Floor, Tile.Wall),
      Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
      Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
    )
  )

  test("exposes a common strategy abstraction for enemy movement decisions") {
    val strategy: EnemyMovementStrategy = DirectPursuitStrategy
    val context = EnemyMovementContext(
      enemyPosition = Position(2, 2),
      playerPosition = Position(2, 4),
      playerPreviousPosition = None,
      map = openMap
    )

    assert(strategy.nextMove(context).contains(Position(2, 3)))
  }

  test("selects only valid orthogonal movement destinations") {
    val moves = EnemyMovement.validMoves(Position(2, 2), mapWithBlockedCorridor)

    assert(moves == Set(Position(1, 2), Position(2, 1), Position(3, 2)))
  }

  test("valid movement selection ignores positions outside the map") {
    val edgeMap = validatedMap(
      Vector(
        Vector(Tile.Floor, Tile.Floor),
        Vector(Tile.Floor, Tile.Wall)
      )
    )

    val moves = EnemyMovement.validMoves(Position(0, 0), edgeMap)

    assert(moves == Set(Position(1, 0), Position(0, 1)))
  }

  test("direct pursuit moves toward the player's current position") {
    val context = EnemyMovementContext(
      enemyPosition = Position(2, 2),
      playerPosition = Position(2, 4),
      playerPreviousPosition = None,
      map = openMap
    )

    assert(DirectPursuitStrategy.nextMove(context).contains(Position(2, 3)))
  }

  test("strategies respect walls when the closest direct move is blocked") {
    val context = EnemyMovementContext(
      enemyPosition = Position(2, 2),
      playerPosition = Position(2, 4),
      playerPreviousPosition = None,
      map = mapWithBlockedCorridor
    )

    assert(DirectPursuitStrategy.nextMove(context).exists(_ != Position(2, 3)))
  }

  test("anticipation targets a predicted player position") {
    val context = EnemyMovementContext(
      enemyPosition = Position(1, 2),
      playerPosition = Position(2, 2),
      playerPreviousPosition = Some(Position(2, 1)),
      map = openMap
    )

    assert(PlayerAnticipationStrategy(stepsAhead = 2).nextMove(context).contains(Position(1, 3)))
  }

  test("pursuit and anticipation produce observably different decisions") {
    val context = EnemyMovementContext(
      enemyPosition = Position(1, 2),
      playerPosition = Position(2, 2),
      playerPreviousPosition = Some(Position(2, 1)),
      map = openMap
    )

    assert(DirectPursuitStrategy.nextMove(context).contains(Position(2, 2)))
    assert(PlayerAnticipationStrategy(stepsAhead = 2).nextMove(context).contains(Position(1, 3)))
  }

  private def validatedMap(rows: Vector[Vector[Tile]]): ValidatedMap =
    ValidatedMap(
      raw = RawMap(rows),
      spawn = Position(1, 1),
      collectibles = Set(Position(1, 2)),
      enemies = Set.empty,
      teleports = Map.empty
    )
end EnemyMovementStrategySpec

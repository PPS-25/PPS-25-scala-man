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
      teleportDisabled = false,
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
      teleportDisabled = false,
      playerPosition = Position(2, 4),
      playerPreviousPosition = None,
      map = openMap
    )

    assert(DirectPursuitStrategy.nextMove(context).contains(Position(2, 3)))
  }

  test("strategies respect walls when the closest direct move is blocked") {
    val context = EnemyMovementContext(
      enemyPosition = Position(2, 2),
      teleportDisabled = false,
      playerPosition = Position(2, 4),
      playerPreviousPosition = None,
      map = mapWithBlockedCorridor
    )

    assert(DirectPursuitStrategy.nextMove(context).exists(_ != Position(2, 3)))
  }

  test("direct pursuit returns no movement when the enemy is trapped") {
    val trappedMap = validatedMap(
      Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall)
      )
    )
    val context = EnemyMovementContext(
      enemyPosition = Position(1, 1),
      teleportDisabled = false,
      playerPosition = Position(1, 2),
      playerPreviousPosition = None,
      map = trappedMap
    )

    assert(DirectPursuitStrategy.nextMove(context).isEmpty)
  }

  test("direct pursuit is deterministic when multiple moves are equally close") {
    val context = EnemyMovementContext(
      enemyPosition = Position(2, 2),
      teleportDisabled = false,
      playerPosition = Position(1, 1),
      playerPreviousPosition = None,
      map = openMap
    )

    assert(DirectPursuitStrategy.nextMove(context).contains(Position(1, 2)))
  }

  test("direct pursuit follows the shortest path around dead ends") {
    val mazeMap = validatedMap(
      Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall, Tile.Floor, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall, Tile.Floor, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
      )
    )
    val context = EnemyMovementContext(
      enemyPosition = Position(3, 2),
      teleportDisabled = false,
      playerPosition = Position(1, 4),
      playerPreviousPosition = None,
      map = mazeMap
    )

    assert(DirectPursuitStrategy.nextMove(context).contains(Position(4, 2)))
  }

  test("direct pursuit uses teleports when they shorten the path to the player") {
    val teleportStart = Position(1, 2)
    val teleportDestination = Position(1, 5)
    val teleportMap = validatedMap(
      rows = Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(
          Tile.Wall,
          Tile.Floor,
          Tile.Teleport(0),
          Tile.Wall,
          Tile.Wall,
          Tile.Teleport(5),
          Tile.Wall
        ),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
      ),
      teleports = Map(0 -> (teleportStart, teleportDestination))
    )
    val context = EnemyMovementContext(
      enemyPosition = Position(1, 1),
      teleportDisabled = false,
      playerPosition = Position(5, 5),
      playerPreviousPosition = None,
      map = teleportMap
    )

    assert(DirectPursuitStrategy.nextMove(context).contains(teleportStart))
  }

  test("anticipation targets a predicted player position") {
    val context = EnemyMovementContext(
      enemyPosition = Position(1, 2),
      teleportDisabled = false,
      playerPosition = Position(2, 2),
      playerPreviousPosition = Some(Position(2, 1)),
      map = openMap
    )

    assert(PlayerAnticipationStrategy(stepsAhead = 2).nextMove(context).contains(Position(1, 3)))
  }

  test("anticipation falls back to the current player position without movement history") {
    val context = EnemyMovementContext(
      enemyPosition = Position(2, 2),
      teleportDisabled = false,
      playerPosition = Position(2, 4),
      playerPreviousPosition = None,
      map = openMap
    )

    assert(PlayerAnticipationStrategy(stepsAhead = 2).nextMove(context).contains(Position(2, 3)))
  }

  test("anticipation respects walls while moving toward the predicted position") {
    val context = EnemyMovementContext(
      enemyPosition = Position(2, 2),
      teleportDisabled = false,
      playerPosition = Position(2, 4),
      playerPreviousPosition = Some(Position(2, 3)),
      map = mapWithBlockedCorridor
    )

    assert(PlayerAnticipationStrategy(stepsAhead = 2).nextMove(context).exists(_ != Position(2, 3)))
  }

  test("anticipation stops prediction at the last walkable cell before a wall") {
    val context = EnemyMovementContext(
      enemyPosition = Position(2, 3),
      teleportDisabled = false,
      playerPosition = Position(1, 4),
      playerPreviousPosition = Some(Position(1, 3)),
      map = openMap
    )

    assert(PlayerAnticipationStrategy(stepsAhead = 2).nextMove(context).contains(Position(1, 3)))
  }

  test("anticipation uses teleports when they shorten the path to the predicted position") {
    val teleportStart = Position(1, 2)
    val teleportDestination = Position(1, 5)
    val teleportMap = validatedMap(
      rows = Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(
          Tile.Wall,
          Tile.Floor,
          Tile.Teleport(0),
          Tile.Wall,
          Tile.Wall,
          Tile.Teleport(5),
          Tile.Wall
        ),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
      ),
      teleports = Map(0 -> (teleportStart, teleportDestination))
    )
    val context = EnemyMovementContext(
      enemyPosition = Position(1, 1),
      teleportDisabled = false,
      playerPosition = Position(5, 5),
      playerPreviousPosition = Some(Position(4, 5)),
      map = teleportMap
    )

    assert(PlayerAnticipationStrategy(stepsAhead = 2).nextMove(context).contains(teleportStart))
  }

  test("anticipation requires a positive prediction distance") {
    assertThrows[IllegalArgumentException] {
      PlayerAnticipationStrategy(stepsAhead = 0)
    }
  }

  test("pursuit and anticipation produce observably different decisions") {
    val context = EnemyMovementContext(
      enemyPosition = Position(1, 2),
      teleportDisabled = false,
      playerPosition = Position(2, 2),
      playerPreviousPosition = Some(Position(2, 1)),
      map = openMap
    )

    assert(DirectPursuitStrategy.nextMove(context).contains(Position(2, 2)))
    assert(PlayerAnticipationStrategy(stepsAhead = 2).nextMove(context).contains(Position(1, 3)))
  }

  test("direct pursuit must leave a teleport before reusing it") {
    val teleportStart = Position(1, 2)
    val teleportDestination = Position(1, 5)
    val teleportMap = validatedMap(
      rows = Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(
          Tile.Wall,
          Tile.Floor,
          Tile.Teleport(0),
          Tile.Floor,
          Tile.Floor,
          Tile.Teleport(5),
          Tile.Wall
        ),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
      ),
      teleports = Map(0 -> (teleportStart, teleportDestination))
    )
    val context = EnemyMovementContext(
      enemyPosition = teleportDestination,
      teleportDisabled = true,
      playerPosition = Position(1, 1),
      playerPreviousPosition = None,
      map = teleportMap
    )

    assert(DirectPursuitStrategy.nextMove(context).contains(Position(1, 4)))
  }

  test("anticipation must leave a teleport before reusing it") {
    val teleportStart = Position(1, 2)
    val teleportDestination = Position(1, 5)
    val teleportMap = validatedMap(
      rows = Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(
          Tile.Wall,
          Tile.Floor,
          Tile.Teleport(0),
          Tile.Floor,
          Tile.Floor,
          Tile.Teleport(5),
          Tile.Wall
        ),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
      ),
      teleports = Map(0 -> (teleportStart, teleportDestination))
    )
    val context = EnemyMovementContext(
      enemyPosition = teleportDestination,
      teleportDisabled = true,
      playerPosition = Position(1, 1),
      playerPreviousPosition = Some(Position(1, 2)),
      map = teleportMap
    )

    assert(PlayerAnticipationStrategy(stepsAhead = 2).nextMove(context).contains(Position(1, 4)))
  }

  private def validatedMap(
      rows: Vector[Vector[Tile]],
      teleports: Map[Int, (Position, Position)] = Map.empty
  ): ValidatedMap =
    ValidatedMap(
      raw = RawMap(rows),
      spawn = Position(1, 1),
      collectibles = Set(Position(1, 2)),
      enemies = Set.empty,
      teleports = teleports
    )
end EnemyMovementStrategySpec

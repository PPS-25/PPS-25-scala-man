package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.Position
import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.map.RawMap
import it.unibo.pps.scalaman.model.map.Tile
import it.unibo.pps.scalaman.model.map.ValidatedMap
import org.scalatest.funsuite.AnyFunSuite

class EnemyStrategySelectorSpec extends AnyFunSuite:
  private val map = ValidatedMap(
    raw = RawMap(
      Vector(
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall),
        Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Floor, Tile.Wall),
        Vector(Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall, Tile.Wall)
      )
    ),
    spawn = Position(1, 1),
    collectibles = Set.empty,
    enemies = Set.empty,
    teleports = Map.empty
  )

  private val context = EnemyMovementContext(
    enemyPosition = Position(1, 2),
    playerPosition = Position(2, 2),
    playerPreviousPosition = Some(Position(2, 1)),
    map = map
  )

  test("selects direct pursuit for hunter enemies") {
    val strategy = EnemyStrategySelector.strategyFor(EnemyKind.Hunter)

    assert(strategy.nextMove(context) == DirectPursuitStrategy.nextMove(context))
  }

  test("selects anticipation for anticipator enemies") {
    val strategy = EnemyStrategySelector.strategyFor(EnemyKind.Anticipator)

    assert(
      strategy.nextMove(context) == PlayerAnticipationStrategy(stepsAhead = 2).nextMove(context)
    )
  }

  test("different enemy kinds can use different strategies") {
    val hunterMove = EnemyStrategySelector.strategyFor(EnemyKind.Hunter).nextMove(context)
    val anticipatorMove = EnemyStrategySelector.strategyFor(EnemyKind.Anticipator).nextMove(context)

    assert(hunterMove.contains(Position(2, 2)))
    assert(anticipatorMove.contains(Position(1, 3)))
    assert(hunterMove != anticipatorMove)
  }
end EnemyStrategySelectorSpec

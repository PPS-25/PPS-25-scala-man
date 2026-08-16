package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.map.EnemyKind

object EnemyStrategySelector:
  def strategyFor(kind: EnemyKind): EnemyMovementStrategy =
    kind match
      case EnemyKind.Hunter      => DirectPursuitStrategy
      case EnemyKind.Anticipator => PlayerAnticipationStrategy(stepsAhead = 2)

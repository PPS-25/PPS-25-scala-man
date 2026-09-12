package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.map.EnemyKind

trait EnemyStrategySelection:
  def strategyFor(kind: EnemyKind): EnemyMovementStrategy

object EnemyStrategySelection:
  def apply(select: EnemyKind => EnemyMovementStrategy): EnemyStrategySelection =
    new EnemyStrategySelection:
      def strategyFor(kind: EnemyKind): EnemyMovementStrategy = select(kind)

object EnemyStrategySelector extends EnemyStrategySelection:
  def strategyFor(kind: EnemyKind): EnemyMovementStrategy =
    kind match
      case EnemyKind.Hunter      => DirectPursuitStrategy
      case EnemyKind.Anticipator => PlayerAnticipationStrategy(stepsAhead = 2)
      case EnemyKind.Patroller   => PatrolStrategy

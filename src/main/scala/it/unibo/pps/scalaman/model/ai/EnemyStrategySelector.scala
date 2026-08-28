package it.unibo.pps.scalaman.model.ai

import it.unibo.pps.scalaman.model.map.EnemyKind

/** Selects the movement strategy to use for an enemy kind. */
trait EnemyStrategySelection:
  def strategyFor(kind: EnemyKind): EnemyMovementStrategy

object EnemyStrategySelection:
  /** Builds a strategy selection from a function, useful for alternative game modes or tests. */
  def apply(select: EnemyKind => EnemyMovementStrategy): EnemyStrategySelection =
    new EnemyStrategySelection:
      def strategyFor(kind: EnemyKind): EnemyMovementStrategy = select(kind)

object EnemyStrategySelector extends EnemyStrategySelection:
  def strategyFor(kind: EnemyKind): EnemyMovementStrategy =
    kind match
      case EnemyKind.Hunter      => DirectPursuitStrategy
      case EnemyKind.Anticipator => PlayerAnticipationStrategy(stepsAhead = 2)

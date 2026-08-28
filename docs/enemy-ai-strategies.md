# Enemy AI Strategies

## Objective

Enemy movement decisions are modeled through interchangeable, deterministic strategies.
The AI model belongs to the domain layer and does not depend on rendering, input handling,
timers, randomness, or UI state.

## Domain Model

`EnemyMovementStrategy` is the common abstraction for enemy decisions:

```scala
trait EnemyMovementStrategy:
  def nextMove(context: EnemyMovementContext): Option[Position]
```

`EnemyMovementContext` contains only immutable domain data:

- the current enemy position;
- the current player position;
- the previous player position, when known;
- the validated map.

The result is an optional next `Position`. `None` means that the enemy has no valid movement.

## Valid Movement

Enemy movement is constrained by `EnemyMovement.validMoves`.

A valid move:

- is orthogonally adjacent to the enemy position;
- is inside the map bounds;
- does not target a `Tile.Wall`.

This rule is shared by every strategy, so pursuit, anticipation, and future strategies obey the
same movement constraints.

## Standard Strategies

### Direct Pursuit

`DirectPursuitStrategy` targets the player's current position.

It chooses the valid adjacent move with the smallest Manhattan distance from the player.
The strategy is deterministic: when candidate moves are equally good, the shared movement ordering
is used.

### Player Anticipation

`PlayerAnticipationStrategy` targets a predicted player position.

The prediction is derived from the difference between the player's current position and previous
position. The strategy projects that movement by `stepsAhead` cells, then chooses the valid adjacent
move that best approaches the predicted target.

If the previous player position is unavailable, anticipation falls back to targeting the current
player position.

## Strategy Selection

`EnemyStrategySelector` maps map-level enemy kinds to domain strategies:

```scala
EnemyKind.Hunter      -> DirectPursuitStrategy
EnemyKind.Anticipator -> PlayerAnticipationStrategy(stepsAhead = 2)
```

`EnemyAiStage` is the default AI stage of the `LevelState` pipeline. It asks the selector for a
strategy only when an enemy is idle, then assigns the corresponding movement. The stage accepts an
`EnemyStrategySelection`, so strategy selection remains independent from rendering and input
handling, and new strategies can be added without changing the game loop.

## Extension Rule

To add a new strategy:

1. implement `EnemyMovementStrategy`;
2. reuse `EnemyMovement.validMoves` or `EnemyMovement.nextMoveToward` when possible;
3. add automated tests for the new behavior;
4. add a mapping in `EnemyStrategySelector` only if the strategy is selected from `EnemyKind`.

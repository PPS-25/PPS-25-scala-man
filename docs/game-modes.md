# Game Modes

## Purpose

Game modes define outcome rules from immutable domain state. They do not depend on rendering,
input handling, wall-clock time, or the generic game update pipeline.

## Common Contract

`GameMode` receives the level progress, remaining collectibles, and immutable game clock, then
returns the current `GameState`. `LevelState` holds the selected mode, which is chosen when the
level is created through `LevelState.from`.

## Implemented Modes

### Normal

Normal mode preserves the standard rules:

- defeat when no lives remain;
- victory when every standard collectible has been collected.

### Timed

Timed mode is configured with a strictly positive duration. It preserves the normal rules until
the clock reaches the limit; at the limit, the level is defeated. Therefore, the last collectible
must be collected strictly before the configured duration expires.

## Planned Extension

Survival mode will use the same contract while replacing the standard completion objective with
survival-time and difficulty-progression rules.

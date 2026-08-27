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

### Survival

Survival mode replaces the standard completion objective with survival time: collecting every item
does not end the level, while losing every life does. The immutable game clock tracks survival time.

Enemy difficulty increases at each configured positive interval by halving the interval between
enemy steps. The interval never becomes lower than the configured positive minimum. The regular
slowdown bonus is applied after this mode-specific interval is calculated.

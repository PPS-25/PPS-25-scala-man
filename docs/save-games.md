# Save games

`GameSaveRepository` is the persistence boundary for a complete `LevelState`. The domain model
does not know the storage format or perform file I/O.

`PropertiesGameSaveRepository` writes a versioned UTF-8 `.properties` file. A save contains a
copy of the ASCII map, rather than a reference to its original file, so it can be resumed after
application restarts even if the original map is moved or edited.

The current format is version 2. Version 2 is not compatible with version 1 because enemy movement
and Survival-mode tuning use the movement-based domain model.

The repository persists the active mode, player state (including an in-progress movement),
enemies (including in-progress movement and teleport state), remaining collectibles, active-effect
durations, lives, score, elapsed time, and game-mode tuning. On load it reconstructs the map
through `MapParser` and `MapValidator`, validates the remaining fields, and returns a
`SaveGameError` for malformed, unsupported, missing, or unreadable data. The View is not involved
in this process.

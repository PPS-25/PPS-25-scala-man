# Save games

`GameSaveRepository` is the persistence boundary for the persistable gameplay progress of a
`LevelState`. The domain model does not know the storage format or perform file I/O.

`PropertiesGameSaveRepository` writes a versioned UTF-8 `.properties` file. A save contains a
copy of the ASCII map, rather than a reference to its original file, so it can be resumed after
application restarts even if the original map is moved or edited.

The current format is version 3. Version 3 is not compatible with version 2 because an enemy also
records the corner it is making for, which a patrolling one needs to carry on its round. Version 3
saves whose Survival speed cap exceeds the current player-speed limit are loaded with that cap
reduced to the supported maximum.

The repository persists the active mode, player state (including an in-progress movement),
enemies (including in-progress movement, teleport state and the corner they make for), remaining
collectibles, active-effect durations, lives, score, elapsed time, and game-mode tuning. On load it reconstructs the map
through `MapParser` and `MapValidator`, validates the remaining fields, and returns a
`SaveGameError` for malformed, unsupported, missing, or unreadable data. The View is not involved
in this process.

## Deliberate limitation

A pending turn request is not persisted. It is transient input intent rather than progress: after loading,
the player remains at the saved position and may choose a direction again. If input buffering becomes part
of the save contract, the format version must be incremented and the requested direction validated on load.

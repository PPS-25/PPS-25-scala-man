# Textual Map Format

This document explains how to create a playable game map in a UTF-8 `.txt`
file. It covers both the ASCII notation and the structural rules checked before
the game accepts a map.

## Goals

The format is intentionally small and deterministic so that loading, parsing,
validation, and generation can be tested independently.

## File structure

- Encoding: UTF-8
- One map per file
- One row per line
- All rows must have the same length
- No extra separators are used between cells

## Walkability

The map is built on a walkable floor grid. The `#` symbol marks the only
non-walkable cell type.

All other symbols represent walkable cells with an overlay:

- player spawn points
- collectibles
- enemies with a specific strategy
- bonuses
- teleports

## Symbols

### Base cells

| Symbol | Meaning |
| --- | --- |
| `#` | Wall, not walkable |
| `.` | Plain walkable floor |
| `S` | Player spawn point on a walkable cell |
| `C` | Collectible on a walkable cell |

### Enemies

| Symbol | Meaning |
| --- | --- |
| `H` | Hunter enemy on a walkable cell |
| `A` | Anticipator enemy on a walkable cell |
| `P` | Patroller enemy on a walkable cell |

### Bonuses

| Symbol | Meaning |
| --- | --- |
| `I` | Invulnerability bonus on a walkable cell |
| `R` | Enemy slowdown bonus on a walkable cell |

### Teleports

| Symbol | Meaning |
| --- | --- |
| `0`-`4` | Teleport cell on a walkable cell |
| `5`-`9` | Paired teleport cell on a walkable cell |

## Creating a valid map

Follow these rules when writing a map:

1. Use a non-empty rectangular grid: every row must have the same length and
   every character must be one of the documented symbols.
2. Surround the grid with `#` walls. Every cell on the outer border must be a
   wall.
3. Place exactly one player spawn, `S`.
4. Place at least one standard collectible, `C`, and at least one enemy
   (`H`, `A`, or `P`). `I` and `R` are optional and may appear multiple times.
5. If teleports are used, place each fixed pair exactly once or omit it
   entirely: `0 <-> 5`, `1 <-> 6`, `2 <-> 7`, `3 <-> 8`, and `4 <-> 9`.
   Teleports are bidirectional.
6. Ensure that every standard collectible and every enemy can be reached from
   `S`, walking orthogonally and, where useful, using teleports. Bonus items
   do not affect whether a map is accepted.

The game rejects empty files, uneven rows, unsupported symbols, open borders,
missing or multiple spawns, missing collectibles or enemies, invalid teleport
pairs, and unreachable required cells. Each problem is reported with an
explanation instead of causing the game to crash.

## Example

```text
#########
#S..0I..#
#..##...#
#..C.5H.#
#..R.A.P#
#########
```

In the example above:

- `S` is the unique spawn point
- `C` is the collectible
- `H` is a hunter enemy
- `A` is an anticipator enemy
- `P` is a patroller enemy
- `I` is an invulnerability bonus
- `R` is an enemy slowdown bonus
- `0` and `5` are a paired teleport couple

## Related

- [`docs/map-generation.md`](map-generation.md) describes how valid maps are
  generated from a specification.

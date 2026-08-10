# Textual Map Format

This document defines the ASCII format used to describe game maps in `.txt`
files.

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

## Rules

- Exactly one `S` must be present.
- At least one `C` must be present.
- At least one enemy symbol (`H` or `A`) must be present.
- `I` and `R` are optional and may appear multiple times.
- Teleports are bidirectional.
- For each fixed pair, the two paired teleport cells must either both appear
  or both be absent.
- Each teleport start symbol (`0`-`4`) may appear at most once.
- Each teleport destination symbol (`5`-`9`) may appear at most once.
- Teleport pairings are fixed: `0 <-> 5`, `1 <-> 6`, `2 <-> 7`, `3 <-> 8`,
  `4 <-> 9`.
- Any symbol outside the table above is invalid.
- Empty files are invalid.
- Ragged maps, where rows have different lengths, are invalid.

## Example

```text
########
#S..0I.#
#..##..#
#..C.5H#
#..R.A.#
########
```

In the example above:

- `S` is the unique spawn point
- `C` is the collectible
- `H` is a hunter enemy
- `A` is an anticipator enemy
- `I` is an invulnerability bonus
- `R` is an enemy slowdown bonus
- `0` and `5` are a paired teleport couple

## Validation boundary

This format document only defines syntactic rules and symbol meaning.
Gameplay rules such as reachability, teleport destination validity, and map
playability are enforced by the validation layer. The validation layer also
interprets the walkable cell underneath each overlay symbol.

## Related

- [`docs/map-generation.md`](map-generation.md) describes how valid maps are
  generated from a specification.

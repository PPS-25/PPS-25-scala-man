# Map Generation

This document describes the contract of the map generator.

## Purpose

The generator builds a `RawMap` from a `MapGenerationSpec` without depending on
parser or gameplay logic. The generated map is meant to be fed into the parser
and validation pipeline.

## Output properties

- the grid is rectangular
- the outer border is made of walls
- the interior is filled with walkable floor cells before overlays are placed
- exactly one spawn is generated
- collectibles, enemies, and teleports are placed according to the requested
  counts
- teleport cells follow the documented code pairs

## Constraints

- width and height must be large enough to host the border and the requested
  entities
- at least one collectible and one enemy are required
- teleport count is limited to the documented code pairs
- generation can be deterministic when a seed is provided

## Notes

The generator intentionally produces structural maps only. Reachability and
playability are verified later by the validation layer.

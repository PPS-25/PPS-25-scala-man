# Map Generation

This document describes how scala-man constructs a playable map from its textual configuration.

## Purpose

Map generation means translating an ASCII map file into the domain map used by a
level. It is deliberately not procedural random-map generation.

## Current use

Shipped and player-selected maps are read from UTF-8 ASCII files. `MapParser`
creates a `RawMap`; `MapValidator` checks its structural and gameplay invariants
and returns a `ValidatedMap` only when the map is playable. The application and
the game domain consume only validated maps.

## Output properties

- the grid is rectangular
- every character maps to a documented tile or overlay
- teleport cells follow the documented code pairs
- a successful validation returns a `ValidatedMap`

## Constraints

- the outer border must be made of walls
- there must be exactly one player spawn, at least one collectible and at least
  one enemy
- collectibles and enemies must be reachable from the spawn, including through
  valid teleports
- every teleport pair must be complete and use a documented code

## Notes

Parsing and validation are deterministic and side-effect free. File access is
kept in the loading boundary; gameplay receives only the resulting validated map.

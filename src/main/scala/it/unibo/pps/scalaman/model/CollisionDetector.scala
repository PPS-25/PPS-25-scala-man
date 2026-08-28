package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.map.{Tile, ValidatedMap}

/** Collision types based on tiles
  */
enum Collision:
  case Wall
  case Enemy
  case Collectible
  case Bonus(kind: Tile)
  case Teleport(code: Int)

object CollisionDetector:
  /** Detects the collisions that would happen on a certain position on a map, without applying any
    * of its gameplay effects. It reports what the tile at that position contains (a wall, a
    * collectible, a bonus, a teleport) and whether any of the enemies currently occupy that
    * position.
    *
    * @param position
    *   the position to check.
    * @param map
    *   the validated map on which to check the tile.
    * @param enemies
    *   where the enemies currently are, to check for a collision on that tile.
    * @return
    *   a set of collisions. Empty if there is no collision.
    */
  def checkForCollision(
      position: Position,
      map: ValidatedMap,
      enemies: Seq[Position]
  ): Set[Collision] =
    val tileCollision =
      if !map.isWalkable(position)
      then Some(Collision.Wall)
      else
        val tileToCheck = map.raw.rows(position.row)(position.col)
        tileToCheck match
          case Tile.Collectible                               => Some(Collision.Collectible)
          case Tile.InvulnerabilityBonus | Tile.SlowdownBonus => Some(Collision.Bonus(tileToCheck))
          case Tile.Teleport(code)                            => Some(Collision.Teleport(code))
          case Tile.Floor | Tile.Spawn | Tile.Hunter | Tile.Anticipator => None
          case Tile.Wall                                                => None
    val enemyCollision =
      if enemies.contains(position)
      then Some(Collision.Enemy)
      else None
    Set(tileCollision, enemyCollision).flatten

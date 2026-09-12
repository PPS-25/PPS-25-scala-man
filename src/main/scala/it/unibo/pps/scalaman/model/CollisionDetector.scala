package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.map.{Tile, ValidatedMap}

enum Collision:
  case Wall
  case Enemy
  case Collectible
  case Bonus(kind: Tile)
  case Teleport(code: Int)

object CollisionDetector:
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
          case Tile.Floor | Tile.Spawn | Tile.Hunter | Tile.Anticipator | Tile.Patroller => None
          case Tile.Wall                                                                 => None
    val enemyCollision =
      if enemies.contains(position)
      then Some(Collision.Enemy)
      else None
    Set(tileCollision, enemyCollision).flatten

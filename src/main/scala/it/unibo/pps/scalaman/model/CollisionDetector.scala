package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.map.validation.MapValidator
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
  /** Detects entities that are occupying a position.
    */
  def detect(position: Position, map: ValidatedMap, enemies: Seq[MovingEntity]): Set[Collision] =
    val tileCollision =
      if !MapValidator.isWalkable(map.raw, position)
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
      if enemies.exists(_.currentPos == position)
      then Some(Collision.Enemy)
      else None
    Set(tileCollision, enemyCollision).flatten

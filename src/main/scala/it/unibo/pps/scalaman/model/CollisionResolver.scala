package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Collision.Teleport
import it.unibo.pps.scalaman.model.map.ValidatedMap

object CollisionResolver:

  /** Resolves one collision by delegating to its corresponding game rule.
    * @param entity
    *   the collision was checked for.
    * @param collision
    *   the type of collision to resolve.
    * @param map
    *   the validated map.
    * @return
    *   the entity after the effect has been applied.
    */
  def resolve(entity: MovingEntity, collision: Collision, map: ValidatedMap): MovingEntity =
    collision match
      case Teleport(c)           => resolveTeleportCollision(entity, c, map)
      case Collision.Wall        => entity
      case Collision.Collectible => entity
      case Collision.Enemy       => entity
      case Collision.Bonus(_)    => entity

  private def resolveTeleportCollision(
      entity: MovingEntity,
      code: Int,
      map: ValidatedMap
  ): MovingEntity =
    val teleportIndex = if code >= 5 then code - 5 else code // finds the index in the teleports map
    map.teleports.get(teleportIndex) match
      case Some((start, dest)) =>
        if entity.currentPos == start
        then entity.copy(currentPos = dest)
        else entity.copy(currentPos = start)
      case None => entity

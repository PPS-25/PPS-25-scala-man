package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Collision.Teleport
import it.unibo.pps.scalaman.model.map.ValidatedMap

object CollisionResolver:

  /** Teleports the player from one end of a teleport to the other.
    */
  def teleported(
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

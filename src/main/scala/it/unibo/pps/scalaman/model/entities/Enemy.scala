package it.unibo.pps.scalaman.model.entities

import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.{Direction, Position}

final case class Enemy(entity: MovingEntity, kind: EnemyKind):
  def currentPos: Position = entity.currentPos
  def facing: Direction = entity.facing

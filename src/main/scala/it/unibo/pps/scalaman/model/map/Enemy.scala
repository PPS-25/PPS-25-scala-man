package it.unibo.pps.scalaman.model.map

import it.unibo.pps.scalaman.model.{Direction, MovingEntity, Position}

final case class Enemy(entity: MovingEntity, kind: EnemyKind):
  def currentPos: Position = entity.currentPos
  def facing: Direction = entity.facing

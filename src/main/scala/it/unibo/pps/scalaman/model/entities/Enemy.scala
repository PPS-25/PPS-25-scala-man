package it.unibo.pps.scalaman.model.entities

import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.{Direction, Position}

final case class Enemy(entity: MovingEntity, kind: EnemyKind, previousPos: Option[Position] = None):
  def currentPos: Position = entity.currentPos
  def facing: Direction = entity.facing
  def moving(step: MovingEntity => MovingEntity): Enemy = copy(entity = step(entity))

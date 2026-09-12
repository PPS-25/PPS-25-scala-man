package it.unibo.pps.scalaman.model.entities

import it.unibo.pps.scalaman.model.Direction.*
import it.unibo.pps.scalaman.model.{Direction, Movement, Position}

import scala.concurrent.duration.{Duration, FiniteDuration}

final case class MovingEntity(
    currentPos: Position,
    facing: Direction,
    timePerPos: FiniteDuration,
    movement: Option[Movement] = None,
    previousPos: Option[Position] = None
):
  require(timePerPos > Duration.Zero, "time per position must be positive")

  def face(direction: Direction): MovingEntity =
    copy(facing = direction)

  def isMoving: Boolean = movement.isDefined

  def meets(other: MovingEntity): Boolean =
    currentPos == other.currentPos || swapping(other) || swappedSinceLastUpdate(other)

  private def swapping(other: MovingEntity): Boolean = (movement, other.movement) match
    case (Some(mine), Some(theirs)) => mine.from == theirs.to && theirs.from == mine.to
    case _                          => false

  private def swappedSinceLastUpdate(other: MovingEntity): Boolean =
    previousPos.contains(other.currentPos) && other.previousPos.contains(currentPos)

  def update(elapsed: FiniteDuration): MovingEntity = movement match
    case Some(m) =>
      val advancementRes = m.advance(elapsed)
      if advancementRes.isComplete then
        copy(currentPos = advancementRes.to, movement = None, previousPos = Some(currentPos))
      else copy(movement = Some(advancementRes), previousPos = None)
    case None => copy(previousPos = None)

  def move(direction: Direction, isWalkable: Position => Boolean): MovingEntity =
    val to = currentPos + direction
    if isWalkable(to) then
      face(direction).copy(
        movement = Some(Movement(currentPos, to, timePerPos)),
        previousPos = None
      )
    else this

package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Direction.*
import scala.concurrent.duration.{Duration, FiniteDuration}

/** A moving entity that occupies a cell and can move towards adjacent ones,
  * moving in one of four directions
  * @param currentCell
  *   the cell that the entity is occupying. It updates on arrival from a
  *   movement.
  * @param facing
  *   the direction the entity is facing.
  * @param timePerCell
  *   the speed of the entity, based on time needed to transition from a cell to
  *   the adjacent one.
  * @param movement
  *   the in-progress movement being performed by the entity.
  */
final case class MovingEntity(
    currentCell: Cell,
    facing: Direction,
    timePerCell: FiniteDuration,
    movement: Option[Movement] = None
):

  /** Makes the entity face a specific direction.
    */
  def face(direction: Direction): MovingEntity =
    copy(facing = direction)

  /** Updates the movement based on the elapsed time. If the updated remaining
    * time is not zero, the entity will keep moving. Otherwise, it will stop.
    * @param elapsed
    *   the elapsed time.
    */
  def update(elapsed: FiniteDuration): MovingEntity = movement match
    case Some(m) =>
      val advancementRes = m.advance(elapsed)
      if advancementRes.isComplete then
        copy(currentCell = advancementRes.to, movement = None)
      else copy(movement = Some(advancementRes))
    case None => this

  /** Makes the entity move towards a specific location, even if that location
    * is not walkable.
    * @param direction
    *   the direction in which to move.
    */
  def move(direction: Direction): MovingEntity =
    face(direction).copy(movement =
      Some(Movement(currentCell, currentCell + direction, timePerCell))
    )

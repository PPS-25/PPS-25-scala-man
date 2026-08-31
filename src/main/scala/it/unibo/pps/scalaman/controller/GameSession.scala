package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.effects.{BonusDuration, Slowdown}
import it.unibo.pps.scalaman.model.{Direction, GameLoop, LevelState, LoopState}

import scala.concurrent.duration.{DurationLong, FiniteDuration}

/** Everything that changes between one frame and the next */
final case class GameSession(
    ticked: Ticked[LevelState, LevelView],
    loop: GameLoop,
    requested: Option[Direction] = None,
    lastFrame: Option[Long] = None
):

  def level: LevelState = ticked.state

  def requestingDirection(direction: Direction): GameSession =
    copy(requested = Some(direction))

  def togglePause: GameSession =
    copy(loop = loop.toggled)

  def isOver: Boolean =
    level.status.isTerminal

  /** The session advanced to a frame. The first frame only records when it happened. */
  def advancedToFrame(nanos: Long)(using BonusDuration, Slowdown): GameSession =
    lastFrame.fold(copy(lastFrame = Some(nanos)))(previous =>
      advancedByDelta((nanos - previous).nanos).copy(lastFrame = Some(nanos))
    )

  /** A direction asked is recorded */
  private def advancedByDelta(delta: FiniteDuration)(using BonusDuration, Slowdown): GameSession =
    if loop.state != LoopState.Running then this
    else
      val input: LevelState => LevelState = requested match
        case Some(direction) => _.playerAsking(direction)
        case None            => identity
      copy(
        ticked = LevelState
          .pipeline(delta, processInput = input)
          .tickNotifying(level, ticked.rendering),
        requested = None
      )

object GameSession:
  def starting(level: LevelState, draw: RenderListener[LevelView]): GameSession =
    val rendering = LevelView.rendering.subscribing(draw)
    GameSession(Ticked(level, rendering.notifying(level)), GameLoop().start())

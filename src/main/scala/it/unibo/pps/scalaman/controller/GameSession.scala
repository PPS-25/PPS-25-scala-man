package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.effects.{BonusDuration, Slowdown}
import it.unibo.pps.scalaman.model.{Direction, GameLoop, LevelState, LoopState}
import it.unibo.pps.scalaman.presentation.{LevelView, RenderListener, Ticked, tickNotifying}

import scala.concurrent.duration.{Duration, DurationInt, DurationLong, FiniteDuration, SECONDS}

final case class GameSession(
    ticked: Ticked[LevelState, LevelView],
    loop: GameLoop,
    lastFrame: Option[Long] = None,
    startsIn: FiniteDuration = GameSession.LeadIn
):

  def level: LevelState = ticked.state

  def requestingDirection(direction: Direction): GameSession =
    copy(ticked = ticked.copy(state = level.playerAsking(direction)))

  def togglePause: GameSession =
    copy(loop = loop.toggled)

  def isOver: Boolean =
    level.status.isTerminal

  def countdown: Option[Int] =
    Option.when(startsIn > Duration.Zero)(
      math.ceil((startsIn - GameSession.GoesFor).toUnit(SECONDS)).toInt.max(0)
    )

  def advancedToFrame(nanos: Long)(using BonusDuration, Slowdown): GameSession =
    lastFrame.fold(copy(lastFrame = Some(nanos)))(previous =>
      advancedBy(nanos - previous).copy(lastFrame = Some(nanos))
    )

  private def advancedBy(nanos: Long)(using BonusDuration, Slowdown): GameSession =
    if loop.state != LoopState.Running then this
    else if startsIn > Duration.Zero then counting(nanos)
    else advancedByDelta(stepOf(nanos))

  private def counting(nanos: Long): GameSession =
    copy(startsIn = (startsIn - nanos.nanos.max(Duration.Zero)).max(Duration.Zero))

  private def stepOf(nanos: Long): FiniteDuration =
    nanos.nanos.min(GameSession.LongestStep).max(Duration.Zero)

  private def advancedByDelta(delta: FiniteDuration)(using BonusDuration, Slowdown): GameSession =
    copy(ticked = LevelState.pipeline(delta).tickNotifying(level, ticked.rendering))

object GameSession:

  val LeadIn: FiniteDuration = 3500.millis

  val GoesFor: FiniteDuration = 500.millis

  val LongestStep: FiniteDuration = 50.millis

  def starting(level: LevelState, draw: RenderListener[LevelView]): GameSession =
    val rendering = LevelView.rendering.subscribing(draw)
    GameSession(Ticked(level, rendering.notifying(level)), GameLoop().start())

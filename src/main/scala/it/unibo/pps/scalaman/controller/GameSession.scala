package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.effects.{BonusDuration, Slowdown}
import it.unibo.pps.scalaman.model.{Direction, GameLoop, LevelState, LoopState}
import it.unibo.pps.scalaman.view.{LevelView, RenderListener, Ticked, tickNotifying}

import scala.concurrent.duration.{Duration, DurationInt, DurationLong, FiniteDuration, SECONDS}

/** Per-game state that changes as animation frames are processed. */
final case class GameSession(
    stateWithRendering: Ticked[LevelState, LevelView],
    loop: GameLoop,
    lastFrame: Option[Long] = None,
    countdownRemaining: FiniteDuration = GameSession.LeadIn
):

  /** The latest domain state, paired with the rendering that was last notified. */
  def level: LevelState = stateWithRendering.state

  /** Stores a turn request until the player reaches a position where it can be applied. */
  def requestDirection(direction: Direction): GameSession =
    copy(stateWithRendering = stateWithRendering.copy(state = level.playerAsking(direction)))

  def togglePause: GameSession =
    copy(loop = loop.toggled)

  def isOver: Boolean =
    level.status.isTerminal

  /** Whole seconds remaining in the lead-in, or none once play has started. */
  def countdown: Option[Int] =
    Option.when(countdownRemaining > Duration.Zero)(
      math.ceil((countdownRemaining - GameSession.GoesFor).toUnit(SECONDS)).toInt.max(0)
    )

  /** Advances to a frame; the first frame only establishes the time origin. */
  def advancedToFrame(nanos: Long)(using BonusDuration, Slowdown): GameSession =
    lastFrame.fold(copy(lastFrame = Some(nanos)))(previous =>
      advanceByElapsedTime(nanos - previous).copy(lastFrame = Some(nanos))
    )

  /** Counts the lead-in in real time, then advances gameplay with capped frame deltas. */
  private def advanceByElapsedTime(nanos: Long)(using BonusDuration, Slowdown): GameSession =
    if loop.state != LoopState.Running then this
    else if countdownRemaining > Duration.Zero then advanceCountdown(nanos)
    else updateGame(gameplayDelta(nanos))

  private def advanceCountdown(nanos: Long): GameSession =
    copy(countdownRemaining =
      (countdownRemaining - nanos.nanos.max(Duration.Zero)).max(Duration.Zero)
    )

  /** Clamps a frame delta to prevent backward or excessively large updates. */
  private def gameplayDelta(nanos: Long): FiniteDuration =
    nanos.nanos.min(GameSession.LongestStep).max(Duration.Zero)

  private def updateGame(delta: FiniteDuration)(using BonusDuration, Slowdown): GameSession =
    copy(
      stateWithRendering = LevelState
        .pipeline(delta)
        .tickNotifying(level, stateWithRendering.rendering)
    )

object GameSession:

  /** Total duration of the pre-game countdown. */
  val LeadIn: FiniteDuration = 3500.millis

  /** Final countdown interval displayed as “Go!”. */
  val GoesFor: FiniteDuration = 500.millis

  /** Maximum gameplay delta, preventing a stalled frame from advancing too much. */
  val LongestStep: FiniteDuration = 50.millis

  def starting(level: LevelState, draw: RenderListener[LevelView]): GameSession =
    val rendering = LevelView.rendering.subscribing(draw)
    GameSession(Ticked(level, rendering.notifying(level)), GameLoop().start())

package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.effects.{BonusDuration, Slowdown}
import it.unibo.pps.scalaman.model.{Direction, GameLoop, LevelState, LoopState}

import scala.concurrent.duration.{Duration, DurationInt, DurationLong, FiniteDuration, SECONDS}

/** Everything that changes between one frame and the next */
final case class GameSession(
    ticked: Ticked[LevelState, LevelView],
    loop: GameLoop,
    lastFrame: Option[Long] = None,
    startsIn: FiniteDuration = GameSession.LeadIn
):

  def level: LevelState = ticked.state

  /** The session after a turn was asked for. The level keeps the request until it can be taken. */
  def requestingDirection(direction: Direction): GameSession =
    copy(ticked = ticked.copy(state = level.playerAsking(direction)))

  def togglePause: GameSession =
    copy(loop = loop.toggled)

  def isOver: Boolean =
    level.status.isTerminal

  /** How many whole seconds are left before the game starts, zero being the moment it goes. Nothing
    * while it is already being played.
    */
  def countdown: Option[Int] =
    Option.when(startsIn > Duration.Zero)(
      math.ceil((startsIn - GameSession.GoesFor).toUnit(SECONDS)).toInt.max(0)
    )

  /** The session advanced to a frame. The first frame only records when it happened. */
  def advancedToFrame(nanos: Long)(using BonusDuration, Slowdown): GameSession =
    lastFrame.fold(copy(lastFrame = Some(nanos)))(previous =>
      advancedBy(nanos - previous).copy(lastFrame = Some(nanos))
    )

  /** A game waiting to start counts the wait down and stands still, so that the count keeps to real
    * time even on a machine dropping frames. Only once it has gone is a frame played, and capped.
    */
  private def advancedBy(nanos: Long)(using BonusDuration, Slowdown): GameSession =
    if loop.state != LoopState.Running then this
    else if startsIn > Duration.Zero then counting(nanos)
    else advancedByDelta(stepOf(nanos))

  private def counting(nanos: Long): GameSession =
    copy(startsIn = (startsIn - nanos.nanos.max(Duration.Zero)).max(Duration.Zero))

  /** How far a game is carried by the time between two frames: never backwards, and never further
    * than a step.
    */
  private def stepOf(nanos: Long): FiniteDuration =
    nanos.nanos.min(GameSession.LongestStep).max(Duration.Zero)

  private def advancedByDelta(delta: FiniteDuration)(using BonusDuration, Slowdown): GameSession =
    copy(ticked = LevelState.pipeline(delta).tickNotifying(level, ticked.rendering))

object GameSession:

  /** How long a game waits before it starts, counting down. */
  val LeadIn: FiniteDuration = 3500.millis

  /** The last stretch of the wait, the one that says go rather than a number. */
  val GoesFor: FiniteDuration = 500.millis

  /** The most a single frame can advance a game. A frame the machine took too long over would
    * otherwise carry everyone across the maze at once, collisions on the way included.
    */
  val LongestStep: FiniteDuration = 50.millis

  def starting(level: LevelState, draw: RenderListener[LevelView]): GameSession =
    val rendering = LevelView.rendering.subscribing(draw)
    GameSession(Ticked(level, rendering.notifying(level)), GameLoop().start())

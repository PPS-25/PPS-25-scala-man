package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.presentation.{LevelView, RenderedMovement}
import it.unibo.pps.scalaman.model.collectibles.Collectible
import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.{GameState, LeaderboardMode, Position}

import scala.concurrent.duration.FiniteDuration

/** Where something is drawn, counted in cells: a whole number sits on a cell, a fraction between
  * two of them.
  */
final case class Spot(row: Double, col: Double)

object Spot:

  /** The spot a still thing is drawn on. */
  def on(position: Position): Spot = Spot(position.row, position.col)

  /** The spot something crossing between two cells has reached. */
  def of(movement: RenderedMovement): Spot = Spot(
    crossed(movement.from.row, movement.to.row, movement.progress),
    crossed(movement.from.col, movement.to.col, movement.progress)
  )

  private def crossed(from: Int, to: Int, progress: Double): Double =
    from + (to - from) * progress

/** One thing drawn over the board, at the spot it has reached. */
final case class Drawn(at: Spot, sprite: Sprite)

/** How the player and the level are doing, drawn beside the maze. */
final case class StatusBar(
    lives: Int,
    remaining: Int,
    applied: Set[BonusEffect],
    state: GameState,
    score: Int,
    elapsed: FiniteDuration,
    timeLeft: Option[FiniteDuration],
    mode: LeaderboardMode = LeaderboardMode.Classic
):

  /** How the player is doing. */
  def playerDescribed: String = s"Lives $lives | Score $score"

  /** How far the level has got, how long it took, and what is in effect. */
  def levelDescribed: String =
    (Seq(mode.label, timeDescribed, s"Left $remaining") ++ effects).mkString(" | ")

  /** The clock the level is read by while it is played: what is left of it when it runs against
    * one, otherwise how long it has been going.
    */
  def timeDescribed: String = spelled(timeLeft.getOrElse(elapsed))

  /** How long the level was played, which is what a game already over is read by: what was left of
    * a clock that ran out says nothing.
    */
  def timePlayed: String = spelled(elapsed)

  private def spelled(duration: FiniteDuration): String =
    val told = duration.toSeconds
    f"${told / SecondsPerMinute}%02d:${told % SecondsPerMinute}%02d"

  private def effects: Option[String] =
    Option.when(applied.nonEmpty)(applied.map(_.toString).toSeq.sorted.mkString(", "))

object StatusBar:

  /** The half of a frame that is read rather than drawn. */
  def of(view: LevelView): StatusBar = StatusBar(
    view.lives,
    view.remaining,
    view.applied,
    view.status,
    view.score,
    view.elapsed,
    view.timeLeft,
    view.mode
  )

private val SecondsPerMinute = 60

/** Whoever moves and whatever is left to pick up, drawn over the board, back to front. */
final case class Frame(entities: Vector[Drawn], status: StatusBar)

object Frame:

  /** What a level shows right now. Things that move no longer share a cell to be sorted by, so what
    * covers what is the order they are drawn in: the player goes last, over everyone.
    */
  def of(view: LevelView): Frame = Frame(
    entities = collectibles(view) ++ enemies(view) :+ player(view),
    status = StatusBar.of(view)
  )

  private def player(view: LevelView): Drawn =
    Drawn(Spot.of(view.player), Sprite.Player(mouth(view.player), view.player.facing))

  // Parity flips at every step, so the mouth moves only while the player does.
  private def mouth(movement: RenderedMovement): Mouth =
    if (movement.from.row + movement.from.col) % 2 == 0 then Mouth.Open else Mouth.Closed

  private def collectibles(view: LevelView): Vector[Drawn] =
    view.collectibles.toVector.map(collectible =>
      Drawn(Spot.on(collectible.position), spriteOf(collectible))
    )

  private def enemies(view: LevelView): Vector[Drawn] =
    view.enemies.map(enemy => Drawn(Spot.of(enemy.at), Sprite.Enemy(enemy.kind)))

  private def spriteOf(collectible: Collectible): Sprite = collectible match
    case Collectible.Basic(_)         => Sprite.Item
    case Collectible.Bonus(_, effect) => Sprite.Bonus(effect)

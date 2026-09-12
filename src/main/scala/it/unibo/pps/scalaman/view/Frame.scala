package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.presentation.{LevelView, RenderedMovement}
import it.unibo.pps.scalaman.model.collectibles.Collectible
import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.{GameState, LeaderboardMode, Position}

import scala.concurrent.duration.FiniteDuration

final case class Spot(row: Double, col: Double)

object Spot:

  def on(position: Position): Spot = Spot(position.row, position.col)

  def of(movement: RenderedMovement): Spot = Spot(
    crossed(movement.from.row, movement.to.row, movement.progress),
    crossed(movement.from.col, movement.to.col, movement.progress)
  )

  private def crossed(from: Int, to: Int, progress: Double): Double =
    from + (to - from) * progress

final case class Drawn(at: Spot, sprite: Sprite)

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

  def playerDescribed: String = s"Lives $lives | Score $score"

  def levelDescribed: String =
    (Seq(mode.label, timeDescribed, s"Left $remaining") ++ effects).mkString(" | ")

  def timeDescribed: String = spelled(timeLeft.getOrElse(elapsed))

  def timePlayed: String = spelled(elapsed)

  private def spelled(duration: FiniteDuration): String =
    val told = duration.toSeconds
    f"${told / SecondsPerMinute}%02d:${told % SecondsPerMinute}%02d"

  private def effects: Option[String] =
    Option.when(applied.nonEmpty)(applied.map(_.toString).toSeq.sorted.mkString(", "))

object StatusBar:

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

final case class Frame(entities: Vector[Drawn], status: StatusBar)

object Frame:

  def of(view: LevelView): Frame = Frame(
    entities = collectibles(view) ++ enemies(view) :+ player(view),
    status = StatusBar.of(view)
  )

  private def player(view: LevelView): Drawn =
    Drawn(Spot.of(view.player), Sprite.Player(mouth(view.player), view.player.facing))

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

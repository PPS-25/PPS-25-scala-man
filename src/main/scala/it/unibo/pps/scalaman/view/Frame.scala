package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.controller.{LevelView, RenderedMovement}
import it.unibo.pps.scalaman.model.collectibles.Collectible
import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.{GameState, Position}

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

/** How the player is doing, drawn beside the maze. */
final case class StatusBar(lives: Int, remaining: Int, applied: Set[BonusEffect], state: GameState):

  def livesDescribed: String = s"Lives $lives"

  def progressDescribed: String = (Seq(s"Left $remaining") ++ effects).mkString(" | ")

  private def effects: Option[String] =
    Option.when(applied.nonEmpty)(applied.map(_.toString).toSeq.sorted.mkString(", "))

/** Whoever moves and whatever is left to pick up, drawn over the board, back to front. */
final case class Frame(entities: Vector[Drawn], status: StatusBar)

object Frame:

  /** What a level shows right now. Things that move no longer share a cell to be sorted by, so what
    * covers what is the order they are drawn in: the player goes last, over everyone.
    */
  def of(view: LevelView): Frame = Frame(
    entities = collectibles(view) ++ enemies(view) :+ player(view),
    status = StatusBar(view.lives, view.remaining, view.applied, view.status)
  )

  private def player(view: LevelView): Drawn =
    Drawn(Spot.of(view.player), Sprite.Player(mouth(view.player)))

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

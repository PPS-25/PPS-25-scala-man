package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.controller.LevelView
import it.unibo.pps.scalaman.model.collectibles.Collectible
import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.{GameState, Position}

/** How the player is doing, drawn beside the maze. */
final case class StatusBar(lives: Int, remaining: Int, applied: Set[BonusEffect], state: GameState):

  def livesDescribed: String = s"Lives $lives"

  def progressDescribed: String = (Seq(s"Left $remaining") ++ effects).mkString(" | ")

  private def effects: Option[String] =
    Option.when(applied.nonEmpty)(applied.map(_.toString).toSeq.sorted.mkString(", "))

/** Whoever moves and whatever is left to pick up, drawn over the board. */
final case class Frame(entities: Map[Position, Sprite], status: StatusBar):

  def at(position: Position): Option[Sprite] = entities.get(position)

object Frame:

  /** What a level shows right now. The order the entities are put in is what covers what: the
    * player is drawn over everyone.
    */
  def of(view: LevelView): Frame = Frame(
    entities = collectibles(view) ++ enemies(view) + (view.player -> player(view)),
    status = StatusBar(view.lives, view.remaining, view.applied, view.status)
  )

  // Parity flips at every step, so the mouth moves only while the player does.
  private def player(view: LevelView): Sprite =
    Sprite.Player(if steppedOnEvenPosition(view) then Mouth.Open else Mouth.Closed)

  private def steppedOnEvenPosition(view: LevelView): Boolean =
    (view.player.row + view.player.col) % 2 == 0

  private def collectibles(view: LevelView): Map[Position, Sprite] =
    view.collectibles.map(collectible => collectible.position -> spriteOf(collectible)).toMap

  private def enemies(view: LevelView): Map[Position, Sprite] =
    view.enemies.map(enemy => enemy.position -> Sprite.Enemy(enemy.kind)).toMap

  private def spriteOf(collectible: Collectible): Sprite = collectible match
    case Collectible.Basic(_)         => Sprite.Item
    case Collectible.Bonus(_, effect) => Sprite.Bonus(effect)

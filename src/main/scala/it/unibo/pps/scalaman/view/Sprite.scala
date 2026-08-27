package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.map.EnemyKind

/** Whether the player is drawn with its mouth open. */
enum Mouth:
  case Open, Closed

/** Everything the game draws on a single position of the map. */
enum Sprite:
  case Wall, Floor, Item
  case Teleport(pair: Int)
  case Player(mouth: Mouth)
  case Bonus(effect: BonusEffect)
  case Enemy(kind: EnemyKind)

object Sprite:

  /** How many teleport doors are told apart by the way they are drawn. */
  val TeleportLooks: Int = 5

  /** An enum with parameterised cases derives no `values`, so the whole set is listed here. */
  val All: Set[Sprite] =
    Set(Wall, Floor, Item) ++
      (0 until TeleportLooks).map(Teleport.apply) ++
      Mouth.values.map(Player.apply) ++
      BonusEffect.values.map(Bonus.apply) ++
      EnemyKind.values.map(Enemy.apply)

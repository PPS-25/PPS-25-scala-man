package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.map.EnemyKind
import scalafx.scene.image.Image

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

/** Which picture belongs to which sprite. The pictures themselves are read only once. */
object SpriteImages:

  /** The file a sprite is drawn from. Teleports past the pictures available reuse the first ones,
    * so that a maze never holds a door that cannot be drawn.
    */
  def fileOf(sprite: Sprite): String = sprite match
    case Sprite.Wall                 => "/wall.png"
    case Sprite.Floor                => "/floor.png"
    case Sprite.Item                 => "/collectible.png"
    case Sprite.Teleport(pair)       => s"/teleport${pair % Sprite.TeleportLooks + 1}.png"
    case Sprite.Player(Mouth.Open)   => "/scalaman1.png"
    case Sprite.Player(Mouth.Closed) => "/scalaman2.png"
    case Sprite.Bonus(BonusEffect.Invulnerability) => "/bonus2.png"
    case Sprite.Bonus(BonusEffect.SlowDown)        => "/bonus1.png"
    case Sprite.Enemy(EnemyKind.Hunter)            => "/enemy1.png"
    case Sprite.Enemy(EnemyKind.Anticipator)       => "/enemy2.png"

  /** The picture of a sprite, read on first use. A missing one is a broken build, and says so. */
  def of(sprite: Sprite): Image = pictures(sprite)

  private lazy val pictures: Map[Sprite, Image] =
    Sprite.All.map(sprite => sprite -> read(fileOf(sprite))).toMap

  // The files are far larger than any cell, and are decoded once at a size worth keeping in memory.
  private def read(file: String): Image =
    Image(
      Option(getClass.getResourceAsStream(file))
        .getOrElse(throw IllegalStateException(s"Missing picture: $file")),
      DecodedAt,
      DecodedAt,
      true,
      true
    )

  private val DecodedAt = 128.0

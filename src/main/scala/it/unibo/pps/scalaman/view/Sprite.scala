package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.Direction
import it.unibo.pps.scalaman.model.map.EnemyKind
import scalafx.scene.image.Image

enum Mouth:
  case Open, Closed

enum Sprite:
  case Wall, Floor, Item
  case Teleport(pair: Int)
  case Player(mouth: Mouth, facing: Direction)
  case Bonus(effect: BonusEffect)
  case Enemy(kind: EnemyKind)

object Sprite:

  val TeleportLooks: Int = 5

  val All: Set[Sprite] =
    Set(Wall, Floor, Item) ++
      (0 until TeleportLooks).map(Teleport.apply) ++
      (for
        mouth <- Mouth.values
        direction <- Direction.values
      yield Player(mouth, direction)) ++
      BonusEffect.values.map(Bonus.apply) ++
      EnemyKind.values.map(Enemy.apply)

object SpriteImages:

  def fileOf(sprite: Sprite): String = sprite match
    case Sprite.Wall                    => "/wall.png"
    case Sprite.Floor                   => "/floor.png"
    case Sprite.Item                    => "/collectible.png"
    case Sprite.Teleport(pair)          => s"/teleport${pair % Sprite.TeleportLooks + 1}.png"
    case Sprite.Player(Mouth.Open, _)   => "/scalaman1.png"
    case Sprite.Player(Mouth.Closed, _) => "/scalaman2.png"
    case Sprite.Bonus(BonusEffect.Invulnerability) => "/bonus2.png"
    case Sprite.Bonus(BonusEffect.SlowDown)        => "/bonus1.png"
    case Sprite.Enemy(EnemyKind.Hunter)            => "/enemy1.png"
    case Sprite.Enemy(EnemyKind.Anticipator)       => "/enemy2.png"
    case Sprite.Enemy(EnemyKind.Patroller)         => "/enemy3.png"

  def of(sprite: Sprite): Image = pictures.getOrElse(sprite, readAndKeep(sprite))

  private var pictures: Map[Sprite, Image] = Map.empty

  private def readAndKeep(sprite: Sprite): Image =
    val picture = read(fileOf(sprite))
    pictures += sprite -> picture
    picture

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

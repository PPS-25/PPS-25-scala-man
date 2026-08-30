package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.map.EnemyKind
import scalafx.scene.image.Image

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

package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.collectibles.Collectible.{Basic, Bonus}
import it.unibo.pps.scalaman.model.collectibles.Collectibles
import it.unibo.pps.scalaman.model.effects.BonusEffect.Invulnerability
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusDuration}
import it.unibo.pps.scalaman.model.map.ValidatedMap
import it.unibo.pps.scalaman.model.Direction.Right
import it.unibo.pps.scalaman.model.entities.MovingEntity

import scala.concurrent.duration.DurationInt

object LevelTestSupport:
  val timePerPos = 100.millis
  val item: Basic = Basic(Position(0, 1))
  val bonus: Bonus = Bonus(Position(0, 2), Invulnerability)
  val lasting = summon[BonusDuration].of(Invulnerability)

  val maze: ValidatedMap = validated("""#######
                                       |#S.C.I#
                                       |#.###.#
                                       |#H...A#
                                       |#######""".stripMargin)

  val mazeWithTeleports: ValidatedMap = validated("""#######
                                                    |#S.C.I#
                                                    |#0###5#
                                                    |#H...A#
                                                    |#######""".stripMargin)
  val teleportStart: Position = Position(2, 1)
  val teleportDestination: Position = Position(2, 5)

  def levelWith(playerAt: Position): LevelState =
    LevelState
      .from(maze)
      .copy(
        player = MovingEntity(playerAt, Right, timePerPos),
        collectibles = Collectibles(Set(item, bonus))
      )

  def teleportLevelWith(playerAt: Position): LevelState =
    levelWith(playerAt).copy(maze = mazeWithTeleports)

  val startingLevel: LevelState = levelWith(Position(0, 0))

  private def validated(text: String): ValidatedMap =
    MapParser
      .parse(text)
      .flatMap(MapValidator.validate)
      .fold(errors => throw IllegalArgumentException(errors.mkString(", ")), identity)

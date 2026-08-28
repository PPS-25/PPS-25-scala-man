package it.unibo.pps.scalaman.persistence

import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.collectibles.Collectible.{Basic, Bonus}
import it.unibo.pps.scalaman.model.collectibles.Collectibles
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusEffect}
import it.unibo.pps.scalaman.model.entities.{Enemy, MovingEntity}
import it.unibo.pps.scalaman.model.map.{EnemyKind, Tile, ValidatedMap}
import it.unibo.pps.scalaman.model.score.ScoreTracker
import it.unibo.pps.scalaman.model.*

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.{Base64, Properties}
import scala.concurrent.duration.{Duration, FiniteDuration, NANOSECONDS}
import scala.util.Using

/** A persistence boundary for complete, resumable game states. */
trait GameSaveRepository:
  def save(level: LevelState, path: Path): Either[SaveGameError, Unit]
  def load(path: Path): Either[SaveGameError, LevelState]

sealed trait SaveGameError
object SaveGameError:
  final case class FileNotFound(path: Path) extends SaveGameError
  final case class ReadFailed(path: Path, message: String) extends SaveGameError
  final case class WriteFailed(path: Path, message: String) extends SaveGameError
  final case class InvalidSave(reason: String) extends SaveGameError

/** A versioned textual save format. It stores a map copy, so a save remains usable even if the
  * source map file is moved or changed between application runs.
  */
final class PropertiesGameSaveRepository private () extends GameSaveRepository:
  import PropertiesGameSaveRepository.*

  def save(level: LevelState, path: Path): Either[SaveGameError, Unit] =
    val properties = encode(level)
    try
      Using.resource(Files.newBufferedWriter(path, StandardCharsets.UTF_8)) { writer =>
        properties.store(writer, "scala-man save game")
      }
      Right(())
    catch case exception: IOException => Left(SaveGameError.WriteFailed(path, exception.getMessage))

  def load(path: Path): Either[SaveGameError, LevelState] =
    if !Files.exists(path) then Left(SaveGameError.FileNotFound(path))
    else
      try
        val properties = new Properties()
        Using.resource(Files.newBufferedReader(path, StandardCharsets.UTF_8)) { reader =>
          properties.load(reader)
        }
        decode(properties)
      catch
        case exception: IOException => Left(SaveGameError.ReadFailed(path, exception.getMessage))
        case exception: IllegalArgumentException =>
          Left(SaveGameError.InvalidSave(exception.getMessage))

  private def encode(level: LevelState): Properties =
    val properties = new Properties()
    properties.setProperty(VersionKey, CurrentVersion)
    properties.setProperty("map", encodeMap(level.maze))
    properties.setProperty("mode", encodeMode(level.mode))
    properties.setProperty("player", encodeEntity(level.player))
    properties.setProperty("enemies", level.enemies.map(encodeEnemy).mkString(";"))
    properties.setProperty(
      "collectibles",
      level.collectibles.placed.toVector.map(encodeCollectible).mkString(";")
    )
    properties.setProperty(
      "effects",
      level.effects.remaining(level.clock.elapsed).map(encodeEffect).mkString(";")
    )
    properties.setProperty("lives", level.progress.lives.toString)
    properties.setProperty("score", s"${level.score.currentScore},${level.score.combo}")
    properties.setProperty("clock", encodeDuration(level.clock.elapsed))
    properties.setProperty(
      "previous-player-position",
      level.playerPreviousPos.map(encodePosition).getOrElse("")
    )
    properties

  private def decode(properties: Properties): Either[SaveGameError, LevelState] =
    for
      _ <- required(properties, VersionKey).flatMap(validateVersion)
      maze <- required(properties, "map").flatMap(decodeMap)
      mode <- required(properties, "mode").flatMap(decodeMode)
      player <- required(properties, "player").flatMap(decodeEntity)
      enemies <- required(properties, "enemies").flatMap(decodeList(_, decodeEnemy))
      collectibles <- required(properties, "collectibles").flatMap(decodeList(_, decodeCollectible))
      effectEntries <- required(properties, "effects").flatMap(decodeList(_, decodeEffect))
      lives <- required(properties, "lives").flatMap(decodeNonNegativeInt(_, "lives"))
      score <- required(properties, "score").flatMap(decodeScore)
      elapsed <- required(properties, "clock").flatMap(decodeNonNegativeDuration(_, "clock"))
      previous <- required(properties, "previous-player-position").flatMap(decodeOptionalPosition)
      effects <- restoreEffects(elapsed, effectEntries)
      _ <- validatePositions(maze, player, enemies, collectibles, previous)
    yield LevelState(
      maze = maze,
      player = player,
      enemies = enemies,
      collectibles = Collectibles(collectibles),
      effects = effects,
      progress = LevelProgress(lives),
      mode = mode,
      score = score,
      clock = GameClock(elapsed),
      playerPreviousPos = previous
    )

object PropertiesGameSaveRepository:
  private val VersionKey = "format-version"
  private val CurrentVersion = "2"

  def apply(): GameSaveRepository = new PropertiesGameSaveRepository()

  private def required(properties: Properties, key: String): Either[SaveGameError, String] =
    Option(properties.getProperty(key)).toRight(SaveGameError.InvalidSave(s"missing '$key'"))

  private def invalid(reason: String): Left[SaveGameError, Nothing] =
    Left(SaveGameError.InvalidSave(reason))

  private def validateVersion(version: String): Either[SaveGameError, Unit] =
    Either.cond(
      version == CurrentVersion,
      (),
      SaveGameError.InvalidSave(s"unsupported format version '$version'")
    )

  private def encodeMap(maze: ValidatedMap): String =
    Base64.getEncoder.encodeToString(
      maze.raw.rows.map(_.map(encodeTile).mkString).mkString("\n").getBytes(StandardCharsets.UTF_8)
    )

  private def decodeMap(value: String): Either[SaveGameError, ValidatedMap] =
    for
      text <- scala.util
        .Try(new String(Base64.getDecoder.decode(value), StandardCharsets.UTF_8))
        .toOption
        .toRight(SaveGameError.InvalidSave("invalid map encoding"))
      raw <- MapParser
        .parse(text)
        .left
        .map(errors => SaveGameError.InvalidSave(errors.mkString(", ")))
      maze <- MapValidator
        .validate(raw)
        .left
        .map(errors => SaveGameError.InvalidSave(errors.mkString(", ")))
    yield maze

  private def encodeTile(tile: Tile): Char = tile match
    case Tile.Wall                 => '#'
    case Tile.Floor                => '.'
    case Tile.Spawn                => 'S'
    case Tile.Collectible          => 'C'
    case Tile.Hunter               => 'H'
    case Tile.Anticipator          => 'A'
    case Tile.InvulnerabilityBonus => 'I'
    case Tile.SlowdownBonus        => 'R'
    case Tile.Teleport(code)       => code.toString.head

  private def encodeMode(mode: GameMode): String = mode match
    case GameMode.Normal                                  => "normal"
    case GameMode.Timed(limit)                            => s"timed,${encodeDuration(limit)}"
    case GameMode.Survival(every, maximumSpeedMultiplier) =>
      s"survival,${encodeDuration(every)},$maximumSpeedMultiplier"

  private def decodeMode(value: String): Either[SaveGameError, GameMode] =
    value.split(",", -1).toList match
      case "normal" :: Nil         => Right(GameMode.Normal)
      case "timed" :: limit :: Nil =>
        decodePositiveDuration(limit, "timed limit").map(GameMode.Timed.apply)
      case "survival" :: every :: maximumSpeedMultiplier :: Nil =>
        for
          difficultyEvery <- decodePositiveDuration(every, "survival difficulty interval")
          maximum <- decodePositiveLong(maximumSpeedMultiplier, "survival maximum speed multiplier")
        yield GameMode.Survival(difficultyEvery, maximum)
      case _ => invalid("invalid game mode")

  private def encodeEntity(entity: MovingEntity): String =
    val movement = entity.movement.map(encodeMovement).getOrElse("")
    val previous = entity.previousPos.map(encodePosition).getOrElse("")
    s"${encodePosition(entity.currentPos)}:${entity.facing}:${encodeDuration(entity.timePerPos)}:$previous:$movement"

  private def decodeEntity(value: String): Either[SaveGameError, MovingEntity] =
    value.split(":", -1).toList match
      case position :: facing :: timePerPos :: previous :: movement :: Nil =>
        for
          currentPosition <- decodeOptionalPosition(position)
            .flatMap(_.toRight(SaveGameError.InvalidSave("missing entity position")))
          direction <- decodeDirection(facing)
          duration <- decodePositiveDuration(timePerPos, "player time per position")
          previousPosition <- decodeOptionalPosition(previous)
          pending <- decodeMovement(movement)
        yield MovingEntity(currentPosition, direction, duration, pending, previousPosition)
      case _ => invalid("invalid player")

  private def encodeMovement(movement: Movement): String =
    s"${encodePosition(movement.from)},${encodePosition(movement.to)},${encodeDuration(movement.remaining)}"

  private def decodeMovement(value: String): Either[SaveGameError, Option[Movement]] =
    if value.isEmpty then Right(None)
    else
      value.split(",", -1).toList match
        case fromRow :: fromCol :: toRow :: toCol :: remaining :: Nil =>
          for
            from <- decodePosition(fromRow, fromCol)
            to <- decodePosition(toRow, toCol)
            duration <- decodePositiveDuration(remaining, "movement remaining time")
          yield Some(Movement(from, to, duration))
        case _ => invalid("invalid movement")

  private def encodeEnemy(enemy: Enemy): String =
    val previous = enemy.previousPos.map(encodePosition).getOrElse("")
    s"${encodeEntity(enemy.entity)}|${enemy.kind}|$previous"

  private def decodeEnemy(value: String): Either[SaveGameError, Enemy] =
    value.split("\\|", -1).toList match
      case entity :: kind :: previous :: Nil =>
        for
          movingEntity <- decodeEntity(entity)
          enemyKind <- decodeEnemyKind(kind)
          previousPosition <- decodeOptionalPosition(previous)
        yield Enemy(movingEntity, enemyKind, previousPosition)
      case _ => invalid("invalid enemy")

  private def encodeCollectible(
      collectible: it.unibo.pps.scalaman.model.collectibles.Collectible
  ): String = collectible match
    case Basic(position)         => s"basic,${encodePosition(position)}"
    case Bonus(position, effect) => s"bonus,${encodePosition(position)},$effect"

  private def decodeCollectible(
      value: String
  ): Either[SaveGameError, it.unibo.pps.scalaman.model.collectibles.Collectible] =
    value.split(",", -1).toList match
      case "basic" :: row :: col :: Nil           => decodePosition(row, col).map(Basic.apply)
      case "bonus" :: row :: col :: effect :: Nil =>
        for
          position <- decodePosition(row, col)
          bonusEffect <- decodeEffectName(effect)
        yield Bonus(position, bonusEffect)
      case _ => invalid("invalid collectible")

  private def encodeEffect(entry: (BonusEffect, FiniteDuration)): String =
    s"${entry._1},${encodeDuration(entry._2)}"

  private def decodeEffect(value: String): Either[SaveGameError, (BonusEffect, FiniteDuration)] =
    value.split(",", -1).toList match
      case effect :: remaining :: Nil =>
        for
          bonusEffect <- decodeEffectName(effect)
          duration <- decodePositiveDuration(remaining, "effect remaining time")
        yield bonusEffect -> duration
      case _ => invalid("invalid active effect")

  private def restoreEffects(
      now: FiniteDuration,
      entries: Vector[(BonusEffect, FiniteDuration)]
  ): Either[SaveGameError, ActiveEffects] =
    Either.cond(
      entries.map(_._1).distinct.size == entries.size,
      ActiveEffects.fromRemaining(now, entries.toMap),
      SaveGameError.InvalidSave("an effect is stored more than once")
    )

  private def decodeList[A](
      value: String,
      decode: String => Either[SaveGameError, A]
  ): Either[SaveGameError, Vector[A]] =
    if value.isEmpty then Right(Vector.empty)
    else
      value
        .split(";", -1)
        .toVector
        .foldRight(Right(Vector.empty): Either[SaveGameError, Vector[A]]) { (entry, decoded) =>
          for value <- decode(entry); tail <- decoded yield value +: tail
        }

  private def encodePosition(position: Position): String = s"${position.row},${position.col}"

  private def decodeOptionalPosition(value: String): Either[SaveGameError, Option[Position]] =
    if value.isEmpty then Right(None)
    else decodePosition(value.split(",", -1).toList).map(Some.apply)

  private def decodePosition(row: String, col: String): Either[SaveGameError, Position] =
    for
      rowNumber <- decodeInt(row, "position row")
      colNumber <- decodeInt(col, "position column")
    yield Position(rowNumber, colNumber)

  private def decodePosition(parts: List[String]): Either[SaveGameError, Position] = parts match
    case row :: col :: Nil => decodePosition(row, col)
    case _                 => invalid("invalid position")

  private def decodeDirection(value: String): Either[SaveGameError, Direction] =
    Direction.values
      .find(_.toString == value)
      .toRight(SaveGameError.InvalidSave("invalid direction"))

  private def decodeEnemyKind(value: String): Either[SaveGameError, EnemyKind] =
    EnemyKind.values
      .find(_.toString == value)
      .toRight(SaveGameError.InvalidSave("invalid enemy kind"))

  private def decodeEffectName(value: String): Either[SaveGameError, BonusEffect] =
    BonusEffect.values
      .find(_.toString == value)
      .toRight(SaveGameError.InvalidSave("invalid bonus effect"))

  private def decodeBoolean(value: String, field: String): Either[SaveGameError, Boolean] =
    value match
      case "true"  => Right(true)
      case "false" => Right(false)
      case _       => invalid(s"invalid $field")

  private def encodeDuration(duration: FiniteDuration): String = duration.toNanos.toString

  private def decodePositiveDuration(
      value: String,
      field: String
  ): Either[SaveGameError, FiniteDuration] =
    decodeDuration(value, field).flatMap { duration =>
      Either.cond(
        duration > Duration.Zero,
        duration,
        SaveGameError.InvalidSave(s"$field must be positive")
      )
    }

  private def decodeNonNegativeDuration(
      value: String,
      field: String
  ): Either[SaveGameError, FiniteDuration] =
    decodeDuration(value, field).flatMap { duration =>
      Either.cond(
        duration >= Duration.Zero,
        duration,
        SaveGameError.InvalidSave(s"$field must not be negative")
      )
    }

  private def decodeDuration(value: String, field: String): Either[SaveGameError, FiniteDuration] =
    scala.util
      .Try(FiniteDuration(value.toLong, NANOSECONDS))
      .toOption
      .toRight(
        SaveGameError.InvalidSave(s"invalid $field")
      )

  private def decodeNonNegativeInt(value: String, field: String): Either[SaveGameError, Int] =
    decodeInt(value, field).flatMap { number =>
      Either.cond(number >= 0, number, SaveGameError.InvalidSave(s"$field must not be negative"))
    }

  private def decodePositiveLong(value: String, field: String): Either[SaveGameError, Long] =
    scala.util
      .Try(value.toLong)
      .toOption
      .flatMap { number =>
        Option.when(number > 0)(number)
      }
      .toRight(SaveGameError.InvalidSave(s"$field must be positive"))

  private def decodeInt(value: String, field: String): Either[SaveGameError, Int] =
    scala.util.Try(value.toInt).toOption.toRight(SaveGameError.InvalidSave(s"invalid $field"))

  private def decodeScore(value: String): Either[SaveGameError, ScoreTracker] =
    value.split(",", -1).toList match
      case score :: combo :: Nil =>
        for
          points <- decodeNonNegativeInt(score, "score")
          streak <- decodeNonNegativeInt(combo, "score combo")
        yield ScoreTracker(points, streak)
      case _ => invalid("invalid score")

  private def validatePositions(
      maze: ValidatedMap,
      player: MovingEntity,
      enemies: Vector[Enemy],
      collectibles: Vector[it.unibo.pps.scalaman.model.collectibles.Collectible],
      previous: Option[Position]
  ): Either[SaveGameError, Unit] =
    def entityPositions(entity: MovingEntity): Vector[Position] =
      entity.movement.toVector.flatMap(movement => Vector(movement.from, movement.to))
    val positions =
      Vector(player.currentPos) ++ player.previousPos ++
        collectibles.map(_.position) ++ previous ++ entityPositions(player) ++
        enemies.flatMap(enemy =>
          Vector(enemy.currentPos) ++ enemy.previousPos ++ enemy.entity.previousPos ++
            entityPositions(enemy.entity)
        )
    Either.cond(
      positions.forall(position => maze.raw.cellAt(position).exists(_.isWalkable)),
      (),
      SaveGameError.InvalidSave("a saved entity is outside the walkable map")
    )

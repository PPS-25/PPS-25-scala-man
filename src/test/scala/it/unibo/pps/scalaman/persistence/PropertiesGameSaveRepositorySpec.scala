package it.unibo.pps.scalaman.persistence

import it.unibo.pps.scalaman.app.MapName
import it.unibo.pps.scalaman.model.*
import it.unibo.pps.scalaman.model.LevelTestSupport.maze
import it.unibo.pps.scalaman.model.effects.BonusEffect.SlowDown
import it.unibo.pps.scalaman.model.effects.ActiveEffects
import it.unibo.pps.scalaman.model.entities.{Enemy, MovingEntity}
import it.unibo.pps.scalaman.model.map.EnemyKind
import it.unibo.pps.scalaman.model.score.ScoreTracker
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Properties
import scala.concurrent.duration.DurationInt
import scala.util.Using

class PropertiesGameSaveRepositorySpec extends AnyFunSuite:
  private val repository = PropertiesGameSaveRepository()
  private val mazeName = MapName("arena")

  private def changedSave(key: String, value: String): java.nio.file.Path =
    val path = Files.createTempFile("scala-man-save", ".properties")
    assert(repository.save(LevelState.from(maze), Some(mazeName), path) == Right(()))
    val properties = new Properties()
    Using.resource(Files.newBufferedReader(path, StandardCharsets.UTF_8))(properties.load)
    properties.setProperty(key, value)
    Using.resource(Files.newBufferedWriter(path, StandardCharsets.UTF_8)):
      properties.store(_, "scala-man save game")
    path

  test("a saved game can be loaded with all its progress") {
    val path = Files.createTempFile("scala-man-save", ".properties")
    val original = LevelState
      .from(maze, GameMode.Timed(2.minutes))
      .copy(
        player = MovingEntity(
          Position(1, 2),
          Direction.Right,
          150.millis,
          Some(Movement(Position(1, 2), Position(1, 3), 50.millis))
        ),
        effects = ActiveEffects.empty.activate(SlowDown, 10.seconds, 5.seconds),
        progress = LevelProgress(2),
        score = ScoreTracker(350, 1),
        clock = GameClock(10.seconds),
        playerPreviousPos = Some(Position(1, 1))
      )

    try
      assert(repository.save(original, Some(mazeName), path) == Right(()))
      assert(repository.load(path) == Right(SavedGame(original, Some(mazeName))))
    finally Files.deleteIfExists(path)
  }

  test("a saved game preserves survival tuning and enemy movement state") {
    val path = Files.createTempFile("scala-man-save", ".properties")
    val original = LevelState
      .from(
        maze,
        GameMode.Survival(difficultyEvery = 10.seconds, maximumSpeedMultiplier = 1.25)
      )
      .copy(
        enemies = Vector(
          Enemy(
            MovingEntity(
              Position(3, 1),
              Direction.Right,
              250.millis,
              Some(Movement(Position(3, 1), Position(3, 2), 75.millis)),
              Some(Position(3, 2))
            ),
            EnemyKind.Hunter,
            Some(Position(3, 3))
          )
        )
      )

    try
      assert(repository.save(original, None, path) == Right(()))
      assert(repository.load(path) == Right(SavedGame(original, None)))
    finally Files.deleteIfExists(path)
  }

  test("a saved game preserves the corner a patroller is making for") {
    val path = Files.createTempFile("scala-man-save", ".properties")
    val original = LevelState
      .from(maze)
      .copy(
        enemies = Vector(
          Enemy(
            MovingEntity(Position(3, 2), Direction.Right, 250.millis),
            EnemyKind.Patroller,
            heading = Some(Position(3, 5))
          )
        )
      )

    try
      assert(repository.save(original, Some(mazeName), path) == Right(()))
      assert(repository.load(path) == Right(SavedGame(original, Some(mazeName))))
    finally Files.deleteIfExists(path)
  }

  test("a corrupted save is rejected safely") {
    val path = Files.createTempFile("scala-man-save", ".properties")
    Files.writeString(path, "not a save", StandardCharsets.UTF_8)

    try assert(repository.load(path).isLeft)
    finally Files.deleteIfExists(path)
  }

  test("malformed properties syntax is rejected without throwing") {
    val path = Files.createTempFile("scala-man-save", ".properties")
    Files.writeString(path, "broken=\\u00GG", StandardCharsets.UTF_8)

    try assert(repository.load(path).isLeft)
    finally Files.deleteIfExists(path)
  }

  test("a missing save is reported without throwing") {
    val path = Files.createTempDirectory("scala-man-save").resolve("missing.properties")

    try assert(repository.load(path) == Left(SaveGameError.FileNotFound(path)))
    finally Files.deleteIfExists(path.getParent)
  }

  test("a save with an unsupported format version is rejected") {
    val path = changedSave("format-version", "999")
    try
      assert(
        repository.load(path) == Left(SaveGameError.InvalidSave("unsupported format version '999'"))
      )
    finally Files.deleteIfExists(path)
  }

  test("a save with an invalid mode is rejected") {
    val path = changedSave("mode", "arcade")
    try assert(repository.load(path) == Left(SaveGameError.InvalidSave("invalid game mode")))
    finally Files.deleteIfExists(path)
  }

  test("a save with a negative number of lives is rejected") {
    val path = changedSave("lives", "-1")
    try
      assert(repository.load(path) == Left(SaveGameError.InvalidSave("lives must not be negative")))
    finally Files.deleteIfExists(path)
  }

  test("a save with an entity outside the map is rejected") {
    val path = changedSave("player", "999,999:Right:200000000::")
    try
      assert(
        repository.load(path) == Left(
          SaveGameError.InvalidSave("a saved entity is outside the walkable map")
        )
      )
    finally Files.deleteIfExists(path)
  }

package it.unibo.pps.scalaman.persistence

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
import scala.concurrent.duration.DurationInt

class PropertiesGameSaveRepositorySpec extends AnyFunSuite:
  private val repository = PropertiesGameSaveRepository()

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
      assert(repository.save(original, path) == Right(()))
      assert(repository.load(path) == Right(original))
    finally Files.deleteIfExists(path)
  }

  test("a saved game preserves survival tuning and enemy movement state") {
    val path = Files.createTempFile("scala-man-save", ".properties")
    val original = LevelState
      .from(
        maze,
        GameMode.Survival(difficultyEvery = 10.seconds, maximumSpeedMultiplier = 4)
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
      assert(repository.save(original, path) == Right(()))
      assert(repository.load(path) == Right(original))
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

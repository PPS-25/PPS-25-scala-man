package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Direction.Right
import it.unibo.pps.scalaman.model.LevelTestSupport.{
  bonus,
  item,
  lasting,
  levelWith,
  maze,
  startingLevel,
  teleportDestination,
  teleportLevelWith,
  teleportStart,
  timePerPos
}
import it.unibo.pps.scalaman.model.collectibles.Collectible.{Basic, Bonus}
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusEffect}
import it.unibo.pps.scalaman.model.effects.BonusEffect.{Invulnerability, SlowDown}
import it.unibo.pps.scalaman.model.map.{Enemy, EnemyKind}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class LevelStateTest extends AnyFunSuite:
  private val fromMaze = LevelState.from(maze)
  private val between = LevelState.BetweenEnemySteps
  private def invulnerable(level: LevelState): LevelState = level.copy(effects =
    ActiveEffects.empty.activate(BonusEffect.Invulnerability, 0.millis, lasting)
  )

  test("a level starts with the player on the spawn of the maze") {
    assert(fromMaze.player.currentPos == maze.spawn)
  }

  test("a level starts with the enemies the maze holds") {
    assert(fromMaze.enemies.toSet == maze.enemies)
  }

  test("a level starts with the standard collectibles the maze holds") {
    assert(fromMaze.collectibles.placed.contains(Basic(Position(1, 3))))
  }

  test("a level reads the bonuses the maze holds") {
    assert(fromMaze.collectibles.placed.contains(Bonus(Position(1, 5), Invulnerability)))
  }

  test("a level counts as remaining only the standard collectibles of the maze") {
    assert(fromMaze.collectibles.remaining == maze.collectibles.size)
  }

  test("a level starts with no effect applied") {
    assert(fromMaze.effects.active(fromMaze.clock.elapsed).isEmpty)
  }

  test("a level starts with the lives the game gives") {
    assert(fromMaze.progress == LevelProgress.initial)
  }

  test("a tick runs the stage the enemies are moved by") {
    val movedAway = LevelState.pipeline(updateAi = _.enemiesStepped(Vector.empty))
    assert(movedAway.tick(startingLevel).enemies.isEmpty)
  }

  test("a tick leaves the enemies alone when no stage moves them") {
    assert(LevelState.pipeline().tick(startingLevel).enemies == startingLevel.enemies)
  }

  test("a tick moves the level clock forward") {
    assert(startingLevel.ticking(timePerPos).clock.elapsed == timePerPos)
  }

  test("standing on nothing collects nothing") {
    assert(startingLevel.collecting.collectibles.remaining == 1)
  }

  test("standing on a standard collectible picks it up") {
    assert(levelWith(item.position).collecting.collectibles.isLevelComplete)
  }

  test("standing on a bonus applies the effect it carries") {
    val collected = levelWith(bonus.position).collecting
    assert(collected.effects.isActive(Invulnerability, collected.clock.elapsed))
  }

  test("an effect is dropped once the level ticked past its expiration") {
    val protectedLevel = levelWith(bonus.position).collecting
    val later = protectedLevel.ticking(lasting).withoutExpiredEffects
    assert(later.effects == ActiveEffects.empty)
  }

  test("an effect is kept while the level has not ticked past its expiration") {
    val protectedLevel = levelWith(bonus.position).collecting
    val soon = protectedLevel.ticking(lasting / 2).withoutExpiredEffects
    assert(soon.effects.isActive(Invulnerability, soon.clock.elapsed))
  }

  test("the player leaves no previous position behind while standing still") {
    assert(startingLevel.movingPlayer(identity).playerPreviousPos.isEmpty)
  }

  test("the player leaves its previous position behind once it changes place") {
    val moved = startingLevel.movingPlayer(_.move(Right, _ => true).update(timePerPos))
    assert(moved.playerPreviousPos.contains(startingLevel.player.currentPos))
  }

  test("the player leaves no previous position behind while still on its way") {
    val moving = startingLevel.movingPlayer(_.move(Right, _ => true))
    assert(moving.playerPreviousPos.isEmpty)
  }

  test("enemies do not step before their interval has passed") {
    assert(!startingLevel.ticking(between - 1.milli).enemyStepDue)
  }

  test("enemies step once their interval has passed") {
    assert(startingLevel.ticking(between).enemyStepDue)
  }

  test("enemies wait twice as long while the slow down is applied") {
    val slowed = startingLevel.copy(effects =
      ActiveEffects.empty.activate(SlowDown, startingLevel.clock.elapsed, lasting)
    )
    assert(!slowed.ticking(between).enemyStepDue)
  }

  test("a stepped enemy starts waiting again") {
    val due = startingLevel.ticking(between)
    assert(!due.enemiesStepped(due.enemies).enemyStepDue)
  }

  test("stepping the enemies puts them where they moved") {
    val moved = startingLevel.enemies.map(enemy => enemy.copy(position = Position(1, 1)))
    assert(startingLevel.enemiesStepped(moved).enemies == moved)
  }

  test("stepping an enemy onto a teleport carries it to the paired end") {
    val enemy = Enemy(Position(1, 1), EnemyKind.Hunter)
    val level = teleportLevelWith(Position(0, 0)).copy(enemies = Vector(enemy))
    val stepped = Vector(enemy.copy(position = teleportStart))

    assert(level.enemiesStepped(stepped).enemies.head.position == teleportDestination)
  }

  test("a teleported enemy must leave the teleport before it can use it again") {
    val enemy = Enemy(Position(1, 1), EnemyKind.Hunter)
    val level = teleportLevelWith(Position(0, 0)).copy(enemies = Vector(enemy))
    val teleported = level.enemiesStepped(Vector(enemy.copy(position = teleportStart))).enemies.head
    val waiting = level.copy(enemies = Vector(teleported)).enemiesStepped(Vector(teleported)).enemies.head
    val exited = level
      .copy(enemies = Vector(waiting))
      .enemiesStepped(Vector(waiting.copy(position = Position(3, 5))))
      .enemies.head

    assert(teleported.teleportDisabled)
    assert(waiting.teleportDisabled)
    assert(!exited.teleportDisabled)
  }

  test("an enemy already standing on a teleport is not sent back without a new step") {
    val enemy = Enemy(teleportDestination, EnemyKind.Hunter)
    val level = teleportLevelWith(Position(0, 0)).copy(enemies = Vector(enemy))

    assert(level.enemiesStepped(Vector(enemy)).enemies.head.position == teleportDestination)
  }

  test("collecting a standard item awards its points") {
    assert(levelWith(item.position).collecting.score.currentScore == 50)
  }

  test("collecting a bonus awards its points") {
    assert(levelWith(bonus.position).collecting.score.currentScore == 100)
  }

  test("collecting nothing leaves the score untouched") {
    assert(startingLevel.collecting.score == startingLevel.score)
  }

  test("defeating an enemy while invulnerable awards its points and takes it off the board") {
    val level = invulnerable(levelWith(Position(3, 1))).afterMeetingEnemies
    assert(level.score.currentScore == 200)
    assert(level.score.combo == 1)
    assert(!level.enemies.exists(_.position == Position(3, 1)))
    assert(level.progress == LevelProgress.initial)
  }

  test("meeting an enemy without invulnerability costs a life") {
    val level = levelWith(Position(3, 1)).afterMeetingEnemies
    assert(level.progress.lives == LevelProgress.initial.lives - 1)
  }

  test("defeating enemies one after the other doubles the points awarded") {
    val first = invulnerable(levelWith(Position(3, 1))).afterMeetingEnemies
    val second = first.copy(player = first.player.copy(currentPos = Position(3, 5)))

    assert(second.afterMeetingEnemies.score.currentScore == 200 + 400)
  }

  test("the combo stands while the invulnerability is still applied") {
    assert(
      invulnerable(
        levelWith(Position(3, 1))
      ).afterMeetingEnemies.withoutExpiredEffects.score.combo == 1
    )
  }

  test("the combo is broken when the invulnerability expires") {
    assert(
      invulnerable(levelWith(Position(3, 1))).afterMeetingEnemies
        .ticking(lasting)
        .withoutExpiredEffects
        .score
        .combo == 0
    )
  }

  test("a teleport carries the player to the other teleport end") {
    assert(
      teleportLevelWith(teleportStart).afterTeleporting.player.currentPos == teleportDestination
    )
  }

  test("a teleport does not send back a player that just got teleported through it") {
    assert(
      teleportLevelWith(
        teleportStart
      ).afterTeleporting.afterTeleporting.player.currentPos == teleportDestination
    )
  }

  test("a teleport starts working again once the player steps off and on againt") {
    assert(
      teleportLevelWith(teleportStart).afterTeleporting
        .movingPlayer(_.copy(currentPos = Position(3, 5)))
        .movingPlayer(_.copy(currentPos = teleportDestination))
        .afterTeleporting
        .player
        .currentPos == teleportStart
    )
  }

  test("an enemy teleported onto the player meets it in the same tick") {
    val enemy = Enemy(Position(1, 1), EnemyKind.Hunter)
    val level = teleportLevelWith(teleportDestination).copy(enemies = Vector(enemy))
    val stepped = Vector(enemy.copy(position = teleportStart))
    val ticked = LevelState.pipeline(updateAi = _.enemiesStepped(stepped)).tick(level)

    assert(ticked.progress.lives == LevelProgress.initial.lives - 1)
  }

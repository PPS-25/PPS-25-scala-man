package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.Collision.Teleport
import it.unibo.pps.scalaman.model.collectibles.Collectible.{Basic, Bonus}
import it.unibo.pps.scalaman.model.collectibles.{
  Collectible,
  Collectibles,
  awardedFor,
  collectedBy,
  grantedBy
}
import it.unibo.pps.scalaman.model.effects.BonusEffect.{Invulnerability, SlowDown}
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusDuration, Slowdown}
import it.unibo.pps.scalaman.model.entities.{Enemy, MovingEntity}
import it.unibo.pps.scalaman.model.map.{EnemySpawn, Tile, ValidatedMap}
import it.unibo.pps.scalaman.model.score.{GameResult, ScoreTracker}
import it.unibo.pps.scalaman.model.score.ScoringEvent.EnemyKill

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

/** A level being played: the maze, who moves on it, what is left to pick up, what the bonuses are
  * doing, and how the player is doing.
  */
final case class LevelState(
    maze: ValidatedMap,
    player: MovingEntity,
    enemies: Vector[Enemy],
    collectibles: Collectibles,
    effects: ActiveEffects,
    progress: LevelProgress,
    score: ScoreTracker = ScoreTracker(),
    clock: GameClock = GameClock(),
    playerPreviousPos: Option[Position] = None
):

  /** Whether the player ran into an enemy. */
  def metAnEnemy: Boolean = enemies.exists(enemy => player.meets(enemy.entity))

  /** The level after meeting an enemy. If an invulnerability is applied, the enemies met are
    * defeated, otherwise a life is lost and everyone is sent back to their spawn.
    */
  def afterMeetingEnemies: LevelState =
    if !metAnEnemy then this
    else if effects.isActive(Invulnerability, clock.elapsed) then defeatingEnemies
    else
      val left = progress.afterCollision(effects, clock.elapsed)
      if left == progress then this else copy(progress = left).respawned

  private def defeatingEnemies: LevelState =
    val (defeated, survivors) = enemies.partition(enemy => player.meets(enemy.entity))
    defeated.foldLeft(copy(enemies = survivors)): (level, _) =>
      level.copy(score = level.score.increaseScore(EnemyKill))

  /** The level after a player was carried by a teleport it stepped on. A teleport does not send
    * back a player that just arrived through it. To be sent back, the player needs to step off the
    * teleport and back on again.
    */
  def afterTeleporting: LevelState =
    CollisionDetector
      .checkForCollision(player.currentPos, maze, enemies.map(_.currentPos))
      .collectFirst { case Teleport(code) =>
        code
      }
      .fold(this): code =>
        val carried = CollisionResolver.teleported(player, code, maze)
        if playerPreviousPos.contains(carried.currentPos)
        then this
        else copy(player = carried, playerPreviousPos = Some(player.currentPos))

  private def respawned: LevelState = copy(
    player = player.copy(currentPos = maze.spawn, movement = None),
    enemies = LevelState.spawnedOn(maze),
    playerPreviousPos = None
  )

  /** How the level is going. Running out of lives on the very last collectible is still a defeat.
    */
  def status: GameState =
    if progress.isOver then GameState.Defeat
    else if collectibles.isLevelComplete then GameState.Victory
    else GameState.Running

  def result(playerName: String): Option[GameResult] =
    Option.when(status.isTerminal)(score.toResult(playerName, progress.lives))

  /** The level after some time has passed. A level that ended stands still. */
  def ticking(delta: FiniteDuration): LevelState =
    if status.isTerminal then this
    else copy(clock = clock.advance(delta))

  /** The level after the player moved, remembering where it came from when it changed place. */
  def movingPlayer(step: MovingEntity => MovingEntity): LevelState =
    val moved = step(player)
    if moved.currentPos == player.currentPos then copy(player = moved)
    else copy(player = moved, playerPreviousPos = Some(player.currentPos))

  /** The level after everyone advanced along the step they were taking. */
  def movingOn(delta: FiniteDuration)(using Slowdown): LevelState =
    val forEnemies: FiniteDuration = effects.enemyDelta(delta, clock.elapsed)
    movingPlayer(_.update(delta))
      .copy(enemies = enemies.map(enemy => enemy.moving(_.update(forEnemies))))

  /** The level after the enemies took their step.
    */
  def enemiesStepped(stepped: Vector[Enemy]): LevelState = copy(
    enemies = stepped
  )

  /** The level after the player picked up what it stands on, effect included. */
  def collecting(using BonusDuration): LevelState =
    val picked = collectibles.collectedBy(player)
    copy(
      collectibles = picked.left,
      effects = effects.grantedBy(picked.element, clock.elapsed),
      score = score.awardedFor(picked.element)
    )

  /** The level with the effects that expired dropped. Manages the combo as well, because a combo
    * can increase only while invulnerability is in effect.
    */
  def withoutExpiredEffects: LevelState =
    val remaining = effects.updated(clock.elapsed)
    copy(
      effects = remaining,
      score = if remaining.isActive(Invulnerability, clock.elapsed) then score else score.resetCombo
    )

object LevelState:

  /** How long the enemies wait between steps when nothing holds them back. */
  val BetweenEnemySteps: FiniteDuration = 250.millis

  /** How long the player takes to cross a position. */
  val PlayerTimePerPos: FiniteDuration = 200.millis

  /** How long the enemy takes to cross a position */
  val EnemyTimePerPos: FiniteDuration = 250.millis

  /** A level about to be played: everyone on their spawn, everything still to pick up. */
  def from(maze: ValidatedMap): LevelState = LevelState(
    maze = maze,
    player = MovingEntity(maze.spawn, Direction.Right, PlayerTimePerPos),
    enemies = spawnedOn(maze),
    collectibles = Collectibles(placedOn(maze)),
    effects = ActiveEffects.empty,
    progress = LevelProgress.initial
  )

  /** The stages a level goes through on each tick: the enemies are moved by the one given here, the
    * ones left out belong to other parts of the game.
    */
  def pipeline(delta: FiniteDuration, updateAi: LevelState => LevelState = identity)(using
      BonusDuration,
      Slowdown
  ): GameStateUpdatePipeline[LevelState] =
    GameStateUpdatePipeline(
      updateAi = whileRunning(updateAi),
      updateMovement = whileRunning(_.ticking(delta).movingOn(delta)),
      resolveCollisions = whileRunning(_.afterMeetingEnemies.afterTeleporting),
      collectItems = whileRunning(_.collecting),
      applyBonuses = whileRunning(_.withoutExpiredEffects)
    )

  /** A stage that a level which already ended goes through untouched. */
  private def whileRunning(stage: LevelState => LevelState): LevelState => LevelState =
    level => if level.status.isTerminal then level else stage(level)

  /** The enemies of a maze, always in the same order: a Set promises none, and whoever pairs
    * something to an enemy would find them swapped after a respawn.
    */
  private def spawnedOn(maze: ValidatedMap): Vector[Enemy] =
    maze.enemies.toVector
      .sortBy(enemy => (enemy.position.row, enemy.position.col))
      .map(spawn =>
        Enemy(MovingEntity(spawn.position, Direction.Right, EnemyTimePerPos), spawn.kind)
      )

  private def placedOn(maze: ValidatedMap): Iterable[Collectible] =
    for
      (row, rowIndex) <- maze.raw.rows.zipWithIndex
      (tile, colIndex) <- row.zipWithIndex
      collectible <- asCollectible(tile, Position(rowIndex, colIndex))
    yield collectible

  private def asCollectible(tile: Tile, position: Position): Option[Collectible] = tile match
    case Tile.Collectible          => Some(Basic(position))
    case Tile.InvulnerabilityBonus => Some(Bonus(position, Invulnerability))
    case Tile.SlowdownBonus        => Some(Bonus(position, SlowDown))
    case _                         => None

package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.collectibles.Collectible.{Basic, Bonus}
import it.unibo.pps.scalaman.model.collectibles.{Collectible, Collectibles, collectedBy, grantedBy}
import it.unibo.pps.scalaman.model.effects.BonusEffect.{Invulnerability, SlowDown}
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusDuration, Slowdown}
import it.unibo.pps.scalaman.model.map.{Enemy, Tile, ValidatedMap}

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
    clock: GameClock = GameClock(),
    playerPreviousPos: Option[Position] = None,
    sinceLastEnemyStep: FiniteDuration = Duration.Zero
):

  /** Whether the player ran into an enemy. */
  def metAnEnemy: Boolean = CollisionDetector
    .checkForCollision(player.currentPos, maze, enemies.map(_.position))
    .contains(Collision.Enemy)

  /** The level after meeting an enemy: a life less, unless invulnerability is applied, and everyone
    * back to their spawn. What was picked up and the effects still running are kept.
    */
  def afterMeetingEnemies: LevelState =
    if !metAnEnemy then this
    else
      val left = progress.afterCollision(effects, clock.elapsed)
      if left == progress then this else copy(progress = left).respawned

  private def respawned: LevelState = copy(
    player = player.copy(currentPos = maze.spawn, movement = None),
    enemies = LevelState.spawnedOn(maze),
    playerPreviousPos = None,
    sinceLastEnemyStep = Duration.Zero
  )

  /** How the level is going. Running out of lives on the very last collectible is still a defeat. */
  def status: GameState =
    if progress.isOver then GameState.Defeat
    else if collectibles.isLevelComplete then GameState.Victory
    else GameState.Running

  /** The level after some time has passed. A level that ended stands still. */
  def ticking(delta: FiniteDuration): LevelState =
    if status.isTerminal then this
    else
      copy(
        clock = clock.advance(delta),
        sinceLastEnemyStep = sinceLastEnemyStep + delta
      )

  /** The level after the player moved, remembering where it came from when it changed place. */
  def movingPlayer(step: MovingEntity => MovingEntity): LevelState =
    val moved = step(player)
    if moved.currentPos == player.currentPos then copy(player = moved)
    else copy(player = moved, playerPreviousPos = Some(player.currentPos))

  /** Whether the enemies are due a step, waiting longer while the slow down is applied. */
  def enemyStepDue(using Slowdown): Boolean =
    sinceLastEnemyStep >= enemyStepInterval

  /** How long the enemies wait between steps right now. */
  def enemyStepInterval(using Slowdown): FiniteDuration =
    effects.enemyStepInterval(LevelState.BetweenEnemySteps, clock.elapsed)

  /** The level after the enemies took their step, keeping what was waited beyond the interval so
    * that they do not fall behind.
    */
  def enemiesStepped(stepped: Vector[Enemy])(using Slowdown): LevelState = copy(
    enemies = stepped,
    sinceLastEnemyStep = (sinceLastEnemyStep - enemyStepInterval).max(Duration.Zero)
  )

  /** The level after the player picked up what it stands on, effect included. */
  def collecting(using BonusDuration): LevelState =
    val picked = collectibles.collectedBy(player)
    copy(
      collectibles = picked.left,
      effects = effects.grantedBy(picked.element, clock.elapsed)
    )

  /** The level with the effects that expired dropped. */
  def withoutExpiredEffects: LevelState = copy(effects = effects.updated(clock.elapsed))

object LevelState:

  /** How long the enemies wait between steps when nothing holds them back. */
  val BetweenEnemySteps: FiniteDuration = 250.millis

  /** How long the player takes to cross a position. */
  val PlayerTimePerPos: FiniteDuration = 200.millis

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
  def pipeline(updateAi: LevelState => LevelState = identity)(using
      BonusDuration
  ): GameStateUpdatePipeline[LevelState] =
    GameStateUpdatePipeline(
      updateAi = whileRunning(updateAi),
      resolveCollisions = whileRunning(_.afterMeetingEnemies),
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
    maze.enemies.toVector.sortBy(enemy => (enemy.position.row, enemy.position.col))

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

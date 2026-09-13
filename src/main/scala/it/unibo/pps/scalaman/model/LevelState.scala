package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.collisions.{
  afterCollision,
  Collision,
  CollisionDetector,
  CollisionResolver
}
import it.unibo.pps.scalaman.model.collisions.Collision.Teleport
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
import it.unibo.pps.scalaman.model.ai.EnemyAiStage
import it.unibo.pps.scalaman.model.entities.{Enemy, MovingEntity}
import it.unibo.pps.scalaman.model.map.{EnemySpawn, Tile, ValidatedMap}
import it.unibo.pps.scalaman.model.score.{GameResult, ScoreTracker}
import it.unibo.pps.scalaman.model.score.ScoringEvent.EnemyKill

import java.time.Instant
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

/** Complete immutable state of a level: map, entities, collectibles, effects, progress, and score.
  */
final case class LevelState(
    maze: ValidatedMap,
    player: MovingEntity,
    requestedDirection: Option[Direction] = None,
    enemies: Vector[Enemy],
    collectibles: Collectibles,
    effects: ActiveEffects,
    progress: LevelProgress,
    mode: GameMode = GameMode.Normal,
    score: ScoreTracker = ScoreTracker(),
    clock: GameClock = GameClock(),
    playerPreviousPos: Option[Position] = None
):

  /** Whether the player currently meets at least one enemy. */
  def metAnEnemy: Boolean = enemies.exists(enemy => player.meets(enemy.entity))

  /** Resolves enemy contact: invulnerability defeats enemies; otherwise the level respawns. */
  def afterMeetingEnemies: LevelState =
    if !metAnEnemy then this
    else if effects.isActive(Invulnerability, clock.elapsed) then defeatingEnemies
    else
      val left = progress.afterCollision(effects, clock.elapsed)
      if left == progress then this else copy(progress = left).respawned

  private def defeatingEnemies: LevelState =
    val (defeated, survivors) = enemies.partition(enemy => player.meets(enemy.entity))
    defeated.foldLeft(copy(enemies = survivors)): (level, _) =>
      level.copy(score = level.score.increaseScore(EnemyKill)(using mode.scoringRule))

  /** Teleports the player once; they must leave the destination before using it again. */
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

  def afterEnemiesTeleporting: LevelState =
    copy(enemies = enemies.map(CollisionResolver.enemyAfterTeleporting(_, maze)))

  private def respawned: LevelState = copy(
    player = player.copy(currentPos = maze.spawn, movement = None, previousPos = None),
    enemies = LevelState.spawnedOn(maze),
    playerPreviousPos = None
  )

  /** Current outcome according to the selected game mode. */
  def status: GameState = mode.status(progress, collectibles, clock)

  /** Score accumulated so far plus the bonus currently awarded by the selected mode. */
  def liveScore: Int =
    score.currentScore + mode
      .bonus(progress, clock, status.isTerminal)
      .fold(0)(mode.scoringRule.awardedPoints(_))

  /** Final result, available only after the level reaches a terminal state. */
  def result(playerName: String, achievedAt: Instant): Option[GameResult] =
    Option.when(status.isTerminal)(
      GameResult(playerName, liveScore, achievedAt)
    )

  /** Advances the clock unless the level has already ended. */
  def ticking(delta: FiniteDuration): LevelState =
    if status.isTerminal then this
    else copy(clock = clock.advance(delta))

  /** Applies player movement and retains the previous cell after an arrival. */
  def movingPlayer(step: MovingEntity => MovingEntity): LevelState =
    val moved = step(player)
    if moved.currentPos == player.currentPos then copy(player = moved)
    else copy(player = moved, playerPreviousPos = Some(player.currentPos))

  /** Records the next requested player direction. */
  def playerAsking(direction: Direction): LevelState =
    copy(requestedDirection = Some(direction))

  /** Starts a player movement only when the requested direction is walkable. */
  private def playerHeading(direction: Direction): LevelState =
    movingPlayer(_.move(direction, maze.isWalkable))

  /** Takes a pending turn when possible, retaining it until the next suitable cell otherwise. */
  def playerStartingNextStep: LevelState = if player.isMoving then this
  else
    val turned = requestedDirection.fold(this)(playerHeading)
    if turned.player.isMoving then turned.copy(requestedDirection = None)
    else playerHeading(player.facing)

  /** Advances the player and enemies along their current movements. */
  def movingOn(delta: FiniteDuration)(using Slowdown): LevelState =
    val forEnemies: FiniteDuration =
      effects.enemyDelta(mode.enemyDelta(delta, clock), clock.elapsed)
    movingPlayer(_.update(delta))
      .copy(enemies = enemies.map(enemy => enemy.moving(_.update(forEnemies))))

  /** Replaces enemies after the AI stage chooses their next steps. */
  def enemiesStepped(stepped: Vector[Enemy]): LevelState = copy(
    enemies = stepped
  )

  /** Collects the item under the player and applies its effect and score. */
  def collecting(using BonusDuration): LevelState =
    val picked = collectibles.collectedBy(player)
    copy(
      collectibles = picked.left,
      effects = effects.grantedBy(picked.element, clock.elapsed),
      score = score.awardedFor(picked.element)(using mode.scoringRule)
    )

  /** Removes expired effects and resets the enemy combo when invulnerability ends. */
  def withoutExpiredEffects: LevelState =
    val remaining = effects.updated(clock.elapsed)
    copy(
      effects = remaining,
      score = if remaining.isActive(Invulnerability, clock.elapsed) then score else score.resetCombo
    )

object LevelState:

  /** How long the player takes to cross a position. */
  val PlayerTimePerPos: FiniteDuration = 200.millis

  /** How long the enemy takes to cross a position */
  val EnemyTimePerPos: FiniteDuration = 250.millis

  /** Creates a new level with entities at spawn and all map collectibles present. */
  def from(maze: ValidatedMap, mode: GameMode = GameMode.Normal): LevelState = LevelState(
    maze = maze,
    player = MovingEntity(maze.spawn, Direction.Right, PlayerTimePerPos),
    enemies = spawnedOn(maze),
    collectibles = Collectibles(placedOn(maze)),
    effects = ActiveEffects.empty,
    progress = LevelProgress.initial,
    mode = mode
  )

  /** Builds the ordered tick pipeline; callers may replace the AI stage for testing. */
  def pipeline(
      delta: FiniteDuration,
      updateAi: LevelState => LevelState = level => EnemyAiStage.stage(level)
  )(using
      BonusDuration,
      Slowdown
  ): GameStateUpdatePipeline[LevelState] =
    GameStateUpdatePipeline(
      updateAi = whileRunning(updateAi),
      updateMovement = whileRunning(_.ticking(delta).movingOn(delta)),
      resolveCollisions =
        whileRunning(_.afterMeetingEnemies.afterTeleporting.afterEnemiesTeleporting),
      collectItems = whileRunning(_.collecting),
      applyBonuses = whileRunning(_.withoutExpiredEffects),
      updateState = whileRunning(_.playerStartingNextStep)
    )

  /** Leaves terminal levels unchanged when reached by later pipeline stages. */
  private def whileRunning(stage: LevelState => LevelState): LevelState => LevelState =
    level => if level.status.isTerminal then level else stage(level)

  /** Uses stable ordering so enemies remain identifiable after a respawn. */
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

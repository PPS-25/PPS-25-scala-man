package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.collectibles.Collectible.{Basic, Bonus}
import it.unibo.pps.scalaman.model.collectibles.{Collectible, Collectibles, collectedBy, grantedBy}
import it.unibo.pps.scalaman.model.effects.BonusEffect.{Invulnerability, SlowDown}
import it.unibo.pps.scalaman.model.effects.{ActiveEffects, BonusDuration, Slowdown}
import it.unibo.pps.scalaman.model.map.{Enemy, Tile, ValidatedMap}

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

/** A level being played: the maze it is played on, who moves on it, what is left to pick up, what
  * the bonuses are doing, how the player is doing, and how long it has been running.
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

  /** The level after some time has passed. */
  def ticking(delta: FiniteDuration): LevelState = copy(
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

  /** The level after the enemies took their step. What was waited beyond the interval is kept, so
    * that the steps do not fall behind on ticks longer than the interval itself.
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

  /** A level about to be played on a maze: everyone on their spawn, everything still to pick up. */
  def from(maze: ValidatedMap): LevelState = LevelState(
    maze = maze,
    player = MovingEntity(maze.spawn, Direction.Right, PlayerTimePerPos),
    enemies = maze.enemies.toVector,
    collectibles = Collectibles(placedOn(maze)),
    effects = ActiveEffects.empty,
    progress = LevelProgress.initial
  )

  /** The stages a level goes through on each tick. The ones left out belong to other parts of the
    * game: the enemies are moved by the stage given here.
    */
  def pipeline(updateAi: LevelState => LevelState = identity)(using
      BonusDuration
  ): GameStateUpdatePipeline[LevelState] =
    GameStateUpdatePipeline(
      updateAi = updateAi,
      collectItems = _.collecting,
      applyBonuses = _.withoutExpiredEffects
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

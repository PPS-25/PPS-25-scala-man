package it.unibo.pps.scalaman.model

/** How far the player got into the level. For now, the lives it has left.
  *
  * @throws IllegalArgumentException
  *   if the lives are negative.
  */
final case class LevelProgress(lives: Int):
  require(lives >= 0, "a player cannot have a negative number of lives")

  /** Whether the level is lost, that is, no life is left. */
  def isOver: Boolean = lives == 0

  /** The progress after losing a life, which a lost level no longer has. */
  def lose: LevelProgress = if isOver then this else copy(lives = lives - 1)

object LevelProgress:
  private val InitialLives: Int = 3

  /** The lives the game gives the player at the beginning of a level. */
  def initial: LevelProgress = LevelProgress(InitialLives)

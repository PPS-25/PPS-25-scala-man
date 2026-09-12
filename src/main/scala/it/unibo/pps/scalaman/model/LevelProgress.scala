package it.unibo.pps.scalaman.model

final case class LevelProgress(lives: Int):
  require(lives >= 0, "a player cannot have a negative number of lives")

  def isOver: Boolean = lives == 0

  def lose: LevelProgress = if isOver then this else copy(lives = lives - 1)

object LevelProgress:
  private val InitialLives: Int = 3

  def initial: LevelProgress = LevelProgress(InitialLives)

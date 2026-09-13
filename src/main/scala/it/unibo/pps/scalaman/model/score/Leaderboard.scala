package it.unibo.pps.scalaman.model.score

import it.unibo.pps.scalaman.model.score.Leaderboard.Cap
import it.unibo.pps.scalaman.utils.Monoid

trait LeaderboardStorage:
  def save(leaderboard: Leaderboard): Either[LeaderboardError, Unit]
  def load(): Either[LeaderboardError, Leaderboard]

final case class Leaderboard private (entries: List[GameResult]):
  /** Record the result after the game ended. */
  def recordEntry(result: GameResult): Leaderboard =
    combine(Leaderboard.of(List(result)))

  /** Combines two leaderboards, ordering them and capping them. */
  def combine(other: Leaderboard): Leaderboard =
    Leaderboard.of(entries ++ other.entries)

  def top(n: Int): List[GameResult] =
    entries.take(n)

object Leaderboard:
  val empty: Leaderboard = Leaderboard(Nil)
  val Cap: Int = 100

  /** A leaderboard holding the best of the given results, ordered. */
  def of(results: List[GameResult]): Leaderboard =
    Leaderboard(results.sorted.take(Cap))

  given Monoid[Leaderboard] with
    def empty: Leaderboard = Leaderboard.empty
    def combine(a: Leaderboard, b: Leaderboard): Leaderboard = a.combine(b)

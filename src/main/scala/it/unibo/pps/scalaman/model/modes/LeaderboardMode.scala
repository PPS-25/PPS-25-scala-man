package it.unibo.pps.scalaman.model.modes

/** A leaderboard category, independent from the tuning values of a game mode. */
enum LeaderboardMode(val label: String):
  case Classic extends LeaderboardMode("Classic")
  case Timed extends LeaderboardMode("Timed")
  case Survival extends LeaderboardMode("Survival")

object LeaderboardMode:

  /** The leaderboard category used by a running game. */
  def of(mode: GameMode): LeaderboardMode = mode match
    case GameMode.Normal      => LeaderboardMode.Classic
    case _: GameMode.Timed    => LeaderboardMode.Timed
    case _: GameMode.Survival => LeaderboardMode.Survival

  /** The leaderboard category selected from the menu. */
  def of(choice: ModeChoice): LeaderboardMode = choice match
    case ModeChoice.Normal   => LeaderboardMode.Classic
    case ModeChoice.Timed    => LeaderboardMode.Timed
    case ModeChoice.Survival => LeaderboardMode.Survival

package it.unibo.pps.scalaman.model

enum LeaderboardMode(val label: String):
  case Classic extends LeaderboardMode("Classic")
  case Timed extends LeaderboardMode("Timed")
  case Survival extends LeaderboardMode("Survival")

object LeaderboardMode:

  def of(mode: GameMode): LeaderboardMode = mode match
    case GameMode.Normal      => LeaderboardMode.Classic
    case _: GameMode.Timed    => LeaderboardMode.Timed
    case _: GameMode.Survival => LeaderboardMode.Survival

  def of(choice: ModeChoice): LeaderboardMode = choice match
    case ModeChoice.Normal   => LeaderboardMode.Classic
    case ModeChoice.Timed    => LeaderboardMode.Timed
    case ModeChoice.Survival => LeaderboardMode.Survival

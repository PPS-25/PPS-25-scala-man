package it.unibo.pps.scalaman.model

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/** The rules a game can be played by, as whoever plays picks between them. A choice carries no
  * numbers of its own: those are tuning, and they live below.
  */
enum ModeChoice:
  case Normal, Timed, Survival

/** Which rules a choice stands for. */
trait ModeTuning:
  def of(choice: ModeChoice): GameMode

object ModeTuning:

  private val AgainstTheClock: FiniteDuration = 2.minutes

  /** The rules the game is played with. */
  given standardModes: ModeTuning with
    def of(choice: ModeChoice): GameMode = choice match
      case ModeChoice.Normal   => GameMode.Normal
      case ModeChoice.Timed    => GameMode.Timed(AgainstTheClock)
      case ModeChoice.Survival => GameMode.Survival()

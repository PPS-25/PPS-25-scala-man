package it.unibo.pps.scalaman.model

import scala.concurrent.duration.{DurationInt, FiniteDuration}

enum ModeChoice:
  case Normal, Timed, Survival

trait ModeTuning:
  def of(choice: ModeChoice): GameMode

object ModeTuning:

  private val AgainstTheClock: FiniteDuration = 2.minutes

  given standardModes: ModeTuning with
    def of(choice: ModeChoice): GameMode = choice match
      case ModeChoice.Normal   => GameMode.Normal
      case ModeChoice.Timed    => GameMode.Timed(AgainstTheClock)
      case ModeChoice.Survival => GameMode.Survival()

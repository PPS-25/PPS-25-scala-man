package it.unibo.pps.scalaman.model

enum GameState:
  case Running, Victory, Defeat

  def isTerminal: Boolean = this match
    case Victory | Defeat => true
    case _                => false

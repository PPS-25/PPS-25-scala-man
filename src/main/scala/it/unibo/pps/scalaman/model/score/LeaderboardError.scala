package it.unibo.pps.scalaman.model.score

import java.nio.file.Path

enum LeaderboardError:
  case FileNotFound(path: Path)
  case ReadException(path: Path, exceptionMsg: String)

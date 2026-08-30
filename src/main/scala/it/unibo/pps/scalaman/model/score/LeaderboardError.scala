package it.unibo.pps.scalaman.model.score

import java.nio.file.Path

enum LeaderboardError:
  case ReadFailed(path: Path, message: String)
  case WriteFailed(path: Path, message: String)
  case Malformed(line: String)

package it.unibo.pps.scalaman.map.io

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.NoSuchFileException

import it.unibo.pps.scalaman.model.map.MapLoadError

object MapLoader:
  def load(path: Path): Either[MapLoadError, String] =
    if !Files.exists(path) then Left(MapLoadError.FileNotFound(path))
    else
      try Right(Files.readString(path, StandardCharsets.UTF_8))
      catch
        case _: NoSuchFileException => Left(MapLoadError.FileNotFound(path))
        case ex: IOException        => Left(MapLoadError.ReadFailed(path, ex.getMessage))

package it.unibo.pps.scalaman.model.map

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

trait MapTestSupport:
  protected def resourcePath(resource: String): Path =
    val url = Option(getClass.getResource(s"/maps/$resource"))
      .getOrElse(throw new IllegalArgumentException(s"Missing test resource: $resource"))
    Paths.get(url.toURI)

  protected def resourceText(resource: String): String =
    Files.readString(resourcePath(resource), StandardCharsets.UTF_8)

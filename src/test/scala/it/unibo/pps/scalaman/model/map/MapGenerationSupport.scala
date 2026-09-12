package it.unibo.pps.scalaman.model.map

sealed trait MapGenerationError
object MapGenerationError:
  final case class InvalidSpecification(reason: String) extends MapGenerationError

final case class MapGenerationSpec(
    width: Int,
    height: Int,
    collectibles: Int,
    teleports: Int,
    enemies: Int = 1,
    seed: Option[Long] = None
)

package it.unibo.pps.scalaman.model.loop

import it.unibo.pps.scalaman.app.{DefaultMaps, MapName}
import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.LevelState
import it.unibo.pps.scalaman.model.map.ValidatedMap
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.{DurationInt, DurationLong, FiniteDuration}

/** Manual performance check, kept outside the regular test suite.
  *
  * It measures newly spawned enemies, the costly case in which every enemy must choose a path.
  */
class TickPerformanceIT extends AnyFunSuite:

  private val FrameBudget: FiniteDuration = 16.millis
  private val Delta: FiniteDuration = 16.millis
  private val WarmUpTicks: Int = 200
  private val MeasuredTicks: Int = 1000

  private def mazeOf(name: MapName): ValidatedMap =
    DefaultMaps
      .textOf(name)
      .flatMap(text => MapParser.parse(text).flatMap(MapValidator.validate).toOption)
      .getOrElse(fail(s"$name is not a maze the game accepts"))

  private def averageTick(level: LevelState): FiniteDuration =
    val pipeline = LevelState.pipeline(Delta)
    (1 to WarmUpTicks).foreach(_ => pipeline.tick(level))
    val startedAt = System.nanoTime()
    val ticked = (1 to MeasuredTicks).map(_ => pipeline.tick(level))
    val elapsed = System.nanoTime() - startedAt
    assert(ticked.sizeIs == MeasuredTicks)
    (elapsed / MeasuredTicks).nanos

  DefaultMaps.All.foreach: maze =>
    test(s"a tick of the $maze maze fits in the time a frame is given") {
      val average = averageTick(LevelState.from(mazeOf(maze)))
      info(
        s"$maze: ${average.toMicros} micros per tick, out of the ${FrameBudget.toMicros} micros a frame is given"
      )
      assert(
        average < FrameBudget,
        s"a tick took $average on average, more than the $FrameBudget a frame is given"
      )
    }

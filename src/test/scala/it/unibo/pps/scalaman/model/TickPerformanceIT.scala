package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.app.{DefaultMaps, MapName}
import it.unibo.pps.scalaman.map.parser.MapParser
import it.unibo.pps.scalaman.map.validation.MapValidator
import it.unibo.pps.scalaman.model.map.ValidatedMap
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.{DurationInt, DurationLong, FiniteDuration}

/** A tick has to fit in the time a frame is given What is measured here is the most pessimistic
  * tick. An enemy asks its strategy where to go only while it stands on a cell, so the pathfinding
  * is paid once every sixteen ticks, since an enemy takes 250ms to cross a cell and a tick is worth
  * 16 ms. Here, the tick is performed on a level that has just started, so every enemy is idle, and
  * thus has to perform the pathfinding operation every time. For this reason, this is a case
  * scenario sufficient to state that the game is going to stay inside the budget no matter how it
  * is played.
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

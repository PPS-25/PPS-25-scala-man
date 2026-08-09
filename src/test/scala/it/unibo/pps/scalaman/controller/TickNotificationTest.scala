package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.controller.RenderingTestSupport.{
  State,
  recorder,
  showingShown,
  start
}
import it.unibo.pps.scalaman.model.GameStateUpdatePipeline
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ListBuffer

class TickNotificationTest extends AnyFunSuite:
  private def pipelineMoving(step: State => State) =
    GameStateUpdatePipeline[State](updateMovement = step)

  private val movingForward = pipelineMoving(_.copy(shown = 2))
  private val tickedForward = movingForward.tickNotifying(start, showingShown)

  /** A rendering that has already shown the initial state, so that what the
    * recorder holds afterwards is only what the tick notified.
    */
  private def alreadyShownTo(
      recorded: ListBuffer[Int],
      listener: RenderListener[Int]
  ) =
    val rendering = showingShown.subscribing(listener).notifying(start)
    recorded.clear()
    rendering

  test("a tick changing the shown information notifies the listener") {
    val (recorded, listener) = recorder()
    movingForward.tickNotifying(start, alreadyShownTo(recorded, listener))
    assert(recorded.toSeq == Seq(2))
  }

  test("a tick changing nothing notifies nobody") {
    val (recorded, listener) = recorder()
    pipelineMoving(identity)
      .tickNotifying(start, alreadyShownTo(recorded, listener))
    assert(recorded.isEmpty)
  }

  test("a tick changing only what the view cannot see notifies nobody") {
    val (recorded, listener) = recorder()
    pipelineMoving(_.copy(hidden = 2))
      .tickNotifying(start, alreadyShownTo(recorded, listener))
    assert(recorded.isEmpty)
  }

  test("a notified tick returns the updated state") {
    assert(tickedForward.state == start.copy(shown = 2))
  }

  test("a notified tick returns a rendering remembering what was shown") {
    assert(tickedForward.rendering.lastShown.contains(2))
  }

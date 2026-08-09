package it.unibo.pps.scalaman.controller

import scala.collection.mutable.ListBuffer

/** Fixtures shared by the suites about rendering notifications. */
object RenderingTestSupport:

  /** A state whose `hidden` part the view is never shown. */
  final case class State(shown: Int, hidden: Int)

  val start: State = State(shown = 1, hidden = 1)

  /** A rendering showing the `shown` part of the state and nothing else. */
  def showingShown: Rendering[State, Int] = Rendering(_.shown)

  /** A listener together with what it was notified of. */
  def recorder(): (ListBuffer[Int], RenderListener[Int]) =
    val recorded = ListBuffer.empty[Int]
    (recorded, recorded.addOne)

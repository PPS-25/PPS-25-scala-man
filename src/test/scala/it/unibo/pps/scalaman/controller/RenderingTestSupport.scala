package it.unibo.pps.scalaman.controller

import scala.collection.mutable.ListBuffer
import it.unibo.pps.scalaman.presentation.{RenderListener, Rendering}

object RenderingTestSupport:

  final case class State(shown: Int, hidden: Int)

  val start: State = State(shown = 1, hidden = 1)

  def showingShown: Rendering[State, Int] = Rendering(_.shown)

  def recorder(): (ListBuffer[Int], RenderListener[Int]) =
    val recorded = ListBuffer.empty[Int]
    (recorded, recorded.addOne)

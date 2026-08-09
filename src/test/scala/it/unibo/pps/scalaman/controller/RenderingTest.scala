package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.controller.RenderingTestSupport.{
  recorder,
  showingShown,
  start
}
import org.scalatest.funsuite.AnyFunSuite

class RenderingTest extends AnyFunSuite:
  private val alreadyShown = showingShown.notifying(start)

  test("the very first update is always a relevant one") {
    assert(showingShown.shownAfter(start).contains(start.shown))
  }

  test("an update leaving the state untouched is not a relevant one") {
    assert(alreadyShown.shownAfter(start).isEmpty)
  }

  test(
    "an update changing only what the view cannot see is not a relevant one"
  ) {
    assert(alreadyShown.shownAfter(start.copy(hidden = 2)).isEmpty)
  }

  test("an update changing the shown information is a relevant one") {
    assert(alreadyShown.shownAfter(start.copy(shown = 2)).contains(2))
  }

  test("a listener is notified with the shown information") {
    val (recorded, listener) = recorder()
    showingShown.subscribing(listener).notifying(start)
    assert(recorded.toSeq == Seq(start.shown))
  }

  test("every subscribed listener is notified") {
    val (firstRecorded, first) = recorder()
    val (secondRecorded, second) = recorder()
    showingShown.subscribing(first).subscribing(second).notifying(start)
    assert(firstRecorded.toSeq == Seq(1) && secondRecorded.toSeq == Seq(1))
  }

  test("a listener is not notified when the update is not a relevant one") {
    val (recorded, listener) = recorder()
    showingShown.subscribing(listener).notifying(start).notifying(start)
    assert(recorded.toSeq == Seq(start.shown))
  }

  test("notifying with no listener subscribed still remembers what was shown") {
    assert(showingShown.notifying(start).shownAfter(start).isEmpty)
  }

  test("subscribing a listener leaves the previous rendering untouched") {
    val (recorded, listener) = recorder()
    showingShown.subscribing(listener)
    showingShown.notifying(start)
    assert(recorded.isEmpty)
  }

  test("notifying leaves the previous rendering untouched") {
    alreadyShown.notifying(start.copy(shown = 2))
    assert(alreadyShown.lastShown.contains(start.shown))
  }

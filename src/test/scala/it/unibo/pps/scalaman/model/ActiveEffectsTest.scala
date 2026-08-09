package it.unibo.pps.scalaman.model

import it.unibo.pps.scalaman.model.BonusEffect.{Invulnerability, SlowDown}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.{Duration, DurationInt}

class ActiveEffectsTest extends AnyFunSuite:
  private val start = 0.seconds
  private val duration = 5.seconds

  private val invulnerable = ActiveEffects.empty
    .activate(Invulnerability, start, duration)

  test("no effect is applied before any bonus is collected") {
    assert(ActiveEffects.empty.active(start).isEmpty)
  }

  test("an activated effect is applied before its expiration") {
    assert(invulnerable.isActive(Invulnerability, start + duration - 1.second))
  }

  test("an activated effect is no longer applied at its expiration") {
    assert(!invulnerable.isActive(Invulnerability, start + duration))
  }

  test("an activated effect is no longer applied after its expiration") {
    assert(!invulnerable.isActive(Invulnerability, start + duration + 1.second))
  }

  test("activating an effect leaves the other effects untouched") {
    assert(!invulnerable.isActive(SlowDown, start))
  }

  test("activating two effects applies both") {
    val both = invulnerable.activate(SlowDown, start, duration)
    assert(both.active(start) == Set(Invulnerability, SlowDown))
  }

  test(
    "activating an already active effect postpones its expiration by the full duration"
  ) {
    val halfway = start + duration / 2
    val renewed = invulnerable.activate(Invulnerability, halfway, duration)
    assert(renewed.isActive(Invulnerability, halfway + duration - 1.second))
  }

  test(
    "an effect activated twice is still applied when the first expiration is reached"
  ) {
    val renewed = invulnerable
      .activate(Invulnerability, start + duration / 2, duration)
    assert(renewed.isActive(Invulnerability, start + duration))
  }

  test("an effect lasting no time at all cannot be granted") {
    assertThrows[IllegalArgumentException](
      ActiveEffects.empty.activate(Invulnerability, start, Duration.Zero)
    )
  }

  test("effects expiring at different instants expire independently") {
    val both = invulnerable.activate(SlowDown, start, duration * 2)
    assert(both.active(start + duration) == Set(SlowDown))
  }

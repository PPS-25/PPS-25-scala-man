package it.unibo.pps.scalaman.model

import org.scalatest.funsuite.AnyFunSuite

class GameStateUpdatePipelineTest extends AnyFunSuite:

  private final case class TraceState(events: Vector[String], value: Int)

  test("A new pipeline should leave the state unchanged when all stages are empty") {
    val pipeline = GameStateUpdatePipeline[TraceState]()
    val initial = TraceState(Vector.empty, 0)

    assert(pipeline.tick(initial) == initial)
  }

  test("A pipeline should execute stages in the declared order") {
    val pipeline = GameStateUpdatePipeline[TraceState](
      processInput = state => state.copy(events = state.events :+ "input"),
      updateAi = state => state.copy(events = state.events :+ "ai"),
      updateMovement = state => state.copy(events = state.events :+ "movement"),
      resolveCollisions = state => state.copy(events = state.events :+ "collisions"),
      collectItems = state => state.copy(events = state.events :+ "items"),
      applyBonuses = state => state.copy(events = state.events :+ "bonuses"),
      updateState = state => state.copy(events = state.events :+ "state")
    )

    val result = pipeline.tick(TraceState(Vector.empty, 0))

    assert(result.events == Vector("input", "ai", "movement", "collisions", "items", "bonuses", "state"))
  }

  test("A pipeline should feed each stage with the result of the previous one") {
    val pipeline = GameStateUpdatePipeline[Int](
      processInput = _ + 1,
      updateAi = _ * 2,
      updateMovement = _ - 3,
      resolveCollisions = _ + 10,
      collectItems = _ / 2,
      applyBonuses = _ + 4,
      updateState = _ * 3
    )

    assert(pipeline.tick(5) == 39)
  }

  test("Run should remain a backward-compatible alias of tick") {
    val pipeline = GameStateUpdatePipeline[Int](updateState = _ + 1)

    assert(pipeline.run(0) == pipeline.tick(0))
  }

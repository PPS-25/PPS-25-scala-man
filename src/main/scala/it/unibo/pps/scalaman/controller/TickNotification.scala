package it.unibo.pps.scalaman.controller

import it.unibo.pps.scalaman.model.GameStateUpdatePipeline

/** The outcome of a notified tick. */
final case class Ticked[S, V](state: S, rendering: Rendering[S, V])

extension [S](pipeline: GameStateUpdatePipeline[S])
  /** Runs one tick, notifying whoever renders if what they see changed. */
  def tickNotifying[V](state: S, rendering: Rendering[S, V]): Ticked[S, V] =
    val updated = pipeline.tick(state)
    Ticked(updated, rendering.notifying(updated))

package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.model.GameStateUpdatePipeline

final case class Ticked[S, V](state: S, rendering: Rendering[S, V])

extension [S](pipeline: GameStateUpdatePipeline[S])
  def tickNotifying[V](state: S, rendering: Rendering[S, V]): Ticked[S, V] =
    val updated = pipeline.tick(state)
    Ticked(updated, rendering.notifying(updated))

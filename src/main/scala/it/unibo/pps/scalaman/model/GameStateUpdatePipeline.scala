package it.unibo.pps.scalaman.model

type GameStateStage[S] = S => S

final case class GameStateUpdatePipeline[S](
    updateAi: GameStateStage[S] = identity[S],
    updateMovement: GameStateStage[S] = identity[S],
    resolveCollisions: GameStateStage[S] = identity[S],
    collectItems: GameStateStage[S] = identity[S],
    applyBonuses: GameStateStage[S] = identity[S],
    updateState: GameStateStage[S] = identity[S]
):

  def tick(initialState: S): S =
    List(
      updateAi,
      updateMovement,
      resolveCollisions,
      collectItems,
      applyBonuses,
      updateState
    ).foldLeft(initialState)((state, stage) => stage(state))

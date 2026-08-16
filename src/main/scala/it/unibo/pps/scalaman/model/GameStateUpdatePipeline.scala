package it.unibo.pps.scalaman.model

/** Ordered pipeline executed on each game tick.
  *
  * The pipeline keeps the domain update logic inside the model and applies each stage in a
  * deterministic order: input, AI, movement, collisions, item collection, bonuses, and final state
  * updates.
  *
  * @param processInput
  *   stage that processes player input
  * @param updateAi
  *   stage that updates enemy or autonomous decisions
  * @param updateMovement
  *   stage that updates entity positions
  * @param resolveCollisions
  *   stage that resolves interactions between entities
  * @param collectItems
  *   stage that handles item collection
  * @param applyBonuses
  *   stage that applies temporary or permanent bonuses
  * @param updateState
  *   final stage that reconciles the resulting state
  */
type GameStateStage[S] = S => S

final case class GameStateUpdatePipeline[S](
    processInput: GameStateStage[S] = identity[S],
    updateAi: GameStateStage[S] = identity[S],
    updateMovement: GameStateStage[S] = identity[S],
    resolveCollisions: GameStateStage[S] = identity[S],
    collectItems: GameStateStage[S] = identity[S],
    applyBonuses: GameStateStage[S] = identity[S],
    updateState: GameStateStage[S] = identity[S]
):

  /** Executes one game tick on the provided state.
    *
    * @param initialState
    *   state at the beginning of the tick
    * @return
    *   the updated state after all stages have been executed
    */
  def tick(initialState: S): S =
    List(
      processInput,
      updateAi,
      updateMovement,
      resolveCollisions,
      collectItems,
      applyBonuses,
      updateState
    ).foldLeft(initialState)((state, stage) => stage(state))

  /** Backward-compatible alias for `tick`.
    *
    * @param initialState
    *   state at the beginning of the update
    * @return
    *   the updated state after all stages have been executed
    */
  def run(initialState: S): S = tick(initialState)

package it.unibo.pps.scalaman.controller

/** Notified with what the view needs to draw. */
type RenderListener[V] = V => Unit

/** Shows the listeners a projection of the game state, and only when an update
  * changes it. The last shown value is remembered, so each state is projected
  * once.
  */
final case class Rendering[S, V](
    project: S => V,
    listeners: Seq[RenderListener[V]] = Seq.empty,
    lastShown: Option[V] = None
):

  /** What to show, when the update changed it. */
  def shownAfter(current: S): Option[V] =
    val shown = project(current)
    Option.when(!lastShown.contains(shown))(shown)

  /** Registers a listener for the updates to come. */
  def subscribing(listener: RenderListener[V]): Rendering[S, V] =
    copy(listeners = listeners :+ listener)

  /** Notifies the listeners, and remembers what it showed them. */
  def notifying(current: S): Rendering[S, V] = shownAfter(current) match
    case Some(shown) =>
      listeners.foreach(_(shown))
      copy(lastShown = Some(shown))
    case None => this

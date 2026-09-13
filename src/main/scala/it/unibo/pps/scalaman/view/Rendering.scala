package it.unibo.pps.scalaman.view

/** Notified with what the view needs to draw. */
type RenderListener[V] = V => Unit

/** Projects state for rendering and notifies listeners only after a visible change. */
final case class Rendering[S, V](
    project: S => V,
    listeners: Seq[RenderListener[V]] = Seq.empty,
    lastShown: Option[V] = None
):
  def shownAfter(current: S): Option[V] =
    val shown = project(current)
    Option.when(!lastShown.contains(shown))(shown)

  def subscribing(listener: RenderListener[V]): Rendering[S, V] =
    copy(listeners = listeners :+ listener)

  def notifying(current: S): Rendering[S, V] = shownAfter(current) match
    case Some(shown) =>
      listeners.foreach(_(shown))
      copy(lastShown = Some(shown))
    case None => this

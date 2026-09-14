package it.unibo.pps.scalaman.utils

import org.scalatest.Assertions

object MonoidLawsTest extends Assertions:

  def lawsHold[A](list: List[A])(using m: Monoid[A]): Unit =
    for a <- list do
      assert(m.combine(a, m.empty) == a)
      assert(m.combine(m.empty, a) == a)

    for a <- list; b <- list; c <- list do
      assert(m.combine(m.combine(a, b), c) == m.combine(a, m.combine(b, c)))

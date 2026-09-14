package it.unibo.pps.scalaman.utils

trait Monoid[A]:
  def empty: A
  def combine(a: A, b: A): A

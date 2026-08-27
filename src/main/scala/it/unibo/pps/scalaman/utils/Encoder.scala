package it.unibo.pps.scalaman.utils

trait Encoder[A]:
  def encode(a: A): String

trait Decoder[A]:
  def decode(text: String): Option[A]

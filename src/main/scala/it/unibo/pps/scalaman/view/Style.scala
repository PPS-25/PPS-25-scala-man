package it.unibo.pps.scalaman.view

object Style:

  val Ink = "#141414"
  val Red = "#d32f2f"
  val Chalk = "#e8e6e3"
  val Night = "#0f1b2d"

  def background: String = s"-fx-background-color: $Ink;"

  def menu: String = s"-fx-background-color: $Night;"

  def veil: String = "-fx-background-color: rgba(20, 20, 20, 0.82);"

  def text(size: Double): String =
    s"-fx-font-size: ${size}px; -fx-font-weight: bold; -fx-text-fill: $Chalk;"

  val Banner = 32.0
  val Heading = 22.0
  val Reading = 18.0
  val Listing = 15.0

  private val ButtonText = 16.0
  private val ButtonWidth = 160.0
  private val ButtonCorner = 8.0

  def paper: String = s"-fx-background-color: $Chalk; -fx-background: $Chalk;"

  def read(size: Double): String =
    s"-fx-font-size: ${size}px; -fx-font-weight: bold; -fx-text-fill: $Ink;"

  def heading(size: Double): String =
    s"-fx-font-size: ${size}px; -fx-font-weight: bold; -fx-text-fill: $Red;"

  def button: String =
    s"-fx-font-size: ${ButtonText}px; -fx-font-weight: bold; -fx-text-fill: $Chalk; " +
      s"-fx-background-color: $Red; -fx-background-radius: ${ButtonCorner}px; " +
      s"-fx-background-insets: 0; -fx-border-color: $Ink; -fx-border-width: 2px; " +
      s"-fx-border-radius: ${ButtonCorner}px; -fx-border-insets: 0; " +
      s"-fx-padding: 8px 16px; -fx-min-width: ${ButtonWidth}px;"

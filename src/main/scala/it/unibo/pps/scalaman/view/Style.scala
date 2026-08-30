package it.unibo.pps.scalaman.view

/** The look the game is dressed in: red and black, with a chalky white for what must stand out. */
object Style:

  val Ink = "#141414"
  val Red = "#d32f2f"
  val Chalk = "#e8e6e3"
  val Night = "#0f1b2d"

  /** The ground everything else is read against. */
  def background: String = s"-fx-background-color: $Ink;"

  /** The ground the game is chosen on, taken from the maze it is played in. */
  def menu: String = s"-fx-background-color: $Night;"

  /** What is read over the board while the game is not being played. */
  def veil: String = "-fx-background-color: rgba(20, 20, 20, 0.82);"

  /** Something to read, as large as the room it is given. */
  def text(size: Double): String =
    s"-fx-font-size: ${size}px; -fx-font-weight: bold; -fx-text-fill: $Chalk;"

  private val ButtonText = 16.0
  private val ButtonWidth = 160.0
  private val ButtonCorner = 8.0

  /** The pale ground a table is read on. A scrolling pane paints its own, under the colour. */
  def paper: String = s"-fx-background-color: $Chalk; -fx-background: $Chalk;"

  /** Something read on the pale ground rather than on the dark one. */
  def read(size: Double): String =
    s"-fx-font-size: ${size}px; -fx-font-weight: bold; -fx-text-fill: $Ink;"

  /** The heading of a column, told apart from what it heads. */
  def heading(size: Double): String =
    s"-fx-font-size: ${size}px; -fx-font-weight: bold; -fx-text-fill: $Red;"

  /** Something to press. Every button is dressed the same, whatever it is asked on top of: the
    * border is rounded exactly as much as the background, so the two never come apart.
    */
  def button: String =
    s"-fx-font-size: ${ButtonText}px; -fx-font-weight: bold; -fx-text-fill: $Chalk; " +
      s"-fx-background-color: $Red; -fx-background-radius: ${ButtonCorner}px; " +
      s"-fx-background-insets: 0; -fx-border-color: $Ink; -fx-border-width: 2px; " +
      s"-fx-border-radius: ${ButtonCorner}px; -fx-border-insets: 0; " +
      s"-fx-padding: 8px 16px; -fx-min-width: ${ButtonWidth}px;"

package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.app.MapName
import it.unibo.pps.scalaman.model.score.Leaderboard
import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, ScrollPane}
import scalafx.scene.layout.{GridPane, VBox}
import scalafx.stage.{Modality, Stage, Window}

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

/** One place in the standings, with everything a finished game carries. */
final case class Standing(place: Int, player: String, score: Int, achievedAt: Instant)

/** The best scores reached on a maze, as they are read. */
object Standings:

  private val When = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

  // A leaderboard keeps its entries ordered and capped, so a place is just an index.
  /** Every place a leaderboard holds, in the order it already keeps them. */
  def of(leaderboard: Leaderboard): Seq[Standing] =
    leaderboard.entries.zipWithIndex
      .map((result, place) =>
        Standing(place + 1, result.playerName, result.score, result.achievedAt)
      )

  /** When a game was played, told where whoever reads it lives. */
  def dated(achievedAt: Instant, where: ZoneId): String =
    When.format(achievedAt.atZone(where))

/** Every score reached on a maze, read in a window of its own. */
object LeaderboardWindow:

  private val Headings = Seq("#", "Player", "Score", "When")
  private val TextSize = 15.0
  private val TitleSize = 22.0
  private val SpacedBy = 12.0
  private val Widest = 400.0
  private val Tallest = 420.0

  /** Opens the standings of a maze over the window they were asked from. */
  def open(maze: MapName, places: Seq[Standing], from: Window): Unit =
    val opened = new Stage
    opened.title = s"Leaderboard - ${maze.value}"
    // Owned, so it closes with the game instead of outliving it, and holds the menu meanwhile.
    opened.initOwner(from)
    opened.initModality(Modality.ApplicationModal)
    opened.scene = new Scene:
      root = new VBox:
        alignment = Pos.Center
        spacing = SpacedBy
        padding = Insets(SpacedBy * 2)
        style = Style.menu
        children = Seq(told(maze.value, Style.heading(TitleSize)), read(places), closing(opened))
    opened.showAndWait()

  private def read(places: Seq[Standing]): scalafx.scene.Node =
    if places.isEmpty then told("No scores yet", Style.text(TextSize))
    else
      new ScrollPane:
        content = tabulated(places)
        fitToWidth = true
        // The viewport is what caps the window: the table inside it can be as long as it likes.
        prefViewportWidth = Widest
        prefViewportHeight = Tallest
        maxWidth = Widest
        maxHeight = Tallest
        style = Style.paper

  private def tabulated(places: Seq[Standing]): GridPane = new GridPane:
    alignment = Pos.Center
    style = Style.paper
    hgap = SpacedBy * 2
    vgap = SpacedBy / 3
    padding = Insets(SpacedBy)
    Headings.zipWithIndex.foreach((heading, column) =>
      add(told(heading, Style.heading(TextSize)), column, 0)
    )
    places.zipWithIndex.foreach((standing, place) =>
      columnsOf(standing).zipWithIndex.foreach((text, column) =>
        add(told(text, Style.read(TextSize)), column, place + 1)
      )
    )

  private def columnsOf(standing: Standing): Seq[String] = Seq(
    s"${standing.place}",
    standing.player,
    s"${standing.score}",
    Standings.dated(standing.achievedAt, ZoneId.systemDefault())
  )

  private def closing(opened: Stage): Button = new Button("Close"):
    style = Style.button
    onAction = _ => opened.close()

  private def told(text: String, dressed: String): Label = new Label(text):
    style = dressed

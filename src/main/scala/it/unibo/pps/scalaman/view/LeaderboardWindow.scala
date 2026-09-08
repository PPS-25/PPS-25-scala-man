package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.app.MapName
import it.unibo.pps.scalaman.model.LeaderboardMode
import it.unibo.pps.scalaman.model.score.Leaderboard
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, ComboBox, Label, ScrollPane}
import scalafx.scene.layout.{GridPane, HBox, VBox}
import scalafx.stage.{Modality, Stage, Window}

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

/** One place in the standings, with everything a finished game carries. */
final case class Standing(place: Int, player: String, score: Int, achievedAt: Instant)

/** The map and game category currently shown by a leaderboard. */
final case class LeaderboardSelection(maze: MapName, mode: LeaderboardMode)

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

  /** The standings for the selected map and game category. */
  def forSelection(
      selection: LeaderboardSelection,
      bestOn: (MapName, LeaderboardMode) => Leaderboard
  ): Seq[Standing] = of(bestOn(selection.maze, selection.mode))

  /** The explanation shown when a selected leaderboard has no entries. */
  def emptyMessage(selection: LeaderboardSelection): String =
    s"No ${selection.mode.label} scores for ${selection.maze.value} yet."

  /** When a game was played, told where whoever reads it lives. */
  def dated(achievedAt: Instant, where: ZoneId): String =
    When.format(achievedAt.atZone(where))

/** Every score reached on a maze, read in a window of its own. */
object LeaderboardWindow:

  private val Headings = Seq("#", "Player", "Score", "When")
  private val SpacedBy = 12.0
  private val Widest = 400.0
  private val Tallest = 420.0

  /** Opens standings that can be filtered by map and game category. */
  def open(
      offered: Seq[MapName],
      initially: LeaderboardSelection,
      bestOn: (MapName, LeaderboardMode) => Leaderboard,
      from: Window
  ): Unit =
    val opened = new Stage
    val maps = new ComboBox[String](ObservableBuffer.from(offered.map(_.value))):
      value = initially.maze.value
    val modes = new ComboBox[LeaderboardMode](ObservableBuffer.from(LeaderboardMode.values.toSeq)):
      value = initially.mode
    val heading = told("", Style.heading(Style.Heading))
    val places = new VBox:
      alignment = Pos.Center
    def selection: LeaderboardSelection =
      LeaderboardSelection(MapName(maps.value()), modes.value())
    def refresh(): Unit =
      val selected = selection
      opened.title = s"Leaderboard - ${selected.maze.value} (${selected.mode.label})"
      heading.text = s"${selected.maze.value} - ${selected.mode.label}"
      places.children = Seq(read(Standings.forSelection(selected, bestOn), selected))
    maps.value.onChange((_, _, _) => refresh())
    modes.value.onChange((_, _, _) => refresh())
    refresh()
    // Owned, so it closes with the game instead of outliving it, and holds the menu meanwhile.
    opened.initOwner(from)
    opened.initModality(Modality.ApplicationModal)
    opened.scene = new Scene:
      root = new VBox:
        alignment = Pos.Center
        spacing = SpacedBy
        padding = Insets(SpacedBy * 2)
        style = Style.menu
        children = Seq(
          heading,
          selectors(maps, modes),
          places,
          closing(opened)
        )
    opened.showAndWait()

  private def read(places: Seq[Standing], selection: LeaderboardSelection): scalafx.scene.Node =
    if places.isEmpty then told(Standings.emptyMessage(selection), Style.text(Style.Listing))
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

  private def selectors(maps: ComboBox[String], modes: ComboBox[LeaderboardMode]): HBox = new HBox:
    alignment = Pos.Center
    spacing = SpacedBy
    children = Seq(labeled("Map", maps), labeled("Mode", modes))

  private def labeled(label: String, control: scalafx.scene.Node): HBox = new HBox:
    alignment = Pos.Center
    spacing = SpacedBy / 2
    children = Seq(told(label, Style.text(Style.Listing)), control)

  private def tabulated(places: Seq[Standing]): GridPane = new GridPane:
    alignment = Pos.Center
    style = Style.paper
    hgap = SpacedBy * 2
    vgap = SpacedBy / 3
    padding = Insets(SpacedBy)
    Headings.zipWithIndex.foreach((heading, column) =>
      add(told(heading, Style.heading(Style.Listing)), column, 0)
    )
    places.zipWithIndex.foreach((standing, place) =>
      columnsOf(standing).zipWithIndex.foreach((text, column) =>
        add(told(text, Style.read(Style.Listing)), column, place + 1)
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

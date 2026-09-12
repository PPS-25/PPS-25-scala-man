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

final case class Standing(place: Int, player: String, score: Int, achievedAt: Instant)

final case class LeaderboardSelection(maze: MapName, mode: LeaderboardMode)

object Standings:

  private val When = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

  def of(leaderboard: Leaderboard): Seq[Standing] =
    leaderboard.entries.zipWithIndex
      .map((result, place) =>
        Standing(place + 1, result.playerName, result.score, result.achievedAt)
      )

  def forSelection(
      selection: LeaderboardSelection,
      bestOn: (MapName, LeaderboardMode) => Leaderboard
  ): Seq[Standing] = of(bestOn(selection.maze, selection.mode))

  def emptyMessage(selection: LeaderboardSelection): String =
    s"No ${selection.mode.label} scores for ${selection.maze.value} yet."

  def dated(achievedAt: Instant, where: ZoneId): String =
    When.format(achievedAt.atZone(where))

object LeaderboardWindow:

  private val Headings = Seq("#", "Player", "Score", "When")
  private val SpacedBy = 12.0
  private val Widest = 400.0
  private val Tallest = 420.0

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
    cancelButton = true
    onAction = _ => opened.close()

  private def told(text: String, dressed: String): Label = new Label(text):
    style = dressed

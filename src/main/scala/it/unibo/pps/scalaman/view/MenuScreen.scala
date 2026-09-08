package it.unibo.pps.scalaman.view

import it.unibo.pps.scalaman.app.{Command, MapName, PlayerName}
import it.unibo.pps.scalaman.model.{LeaderboardMode, ModeChoice}
import it.unibo.pps.scalaman.model.effects.BonusEffect
import it.unibo.pps.scalaman.model.score.Leaderboard
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos, Rectangle2D}
import scalafx.scene.Parent
import scalafx.scene.control.{Button, ComboBox, ListView, TextField}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{HBox, VBox}
import scalafx.stage.FileChooser

import java.nio.file.Path

/** The screen a game is started from: who is playing, on which maze, and how others did on it. */
final class MenuScreen(
    offered: Seq[MapName],
    bestOn: (MapName, LeaderboardMode) => Leaderboard,
    playerName: Option[PlayerName],
    chosen: Command => Unit
):

  import MenuScreen.*

  private val player = new TextField:
    promptText = "Your name"
    maxWidth = FieldWidth
    text = playerName.fold("")(_.value)

  private val modes = new ComboBox[ModeChoice](ObservableBuffer.from(ModeChoice.values.toSeq)):
    maxWidth = FieldWidth
    prefWidth = FieldWidth
    value = ModeChoice.Normal

  private val mazes = new ListView[String](ObservableBuffer.from(offered.map(_.value))):
    maxWidth = FieldWidth
    maxHeight = ListHeight

  private val standings = new Button("View leaderboard"):
    style = Style.button
    onAction = _ => showStandings()

  private val play = new Button("Play"):
    onAction = _ =>
      chosenMap.foreach(maze => chosen(Command.StartGame(maze, PlayerName(named), chosenMode)))
    style = Style.button

  private val loadMap = new Button("Load map..."):
    onAction = _ =>
      picked("Open a maze").foreach(path =>
        chosen(Command.LoadMap(path, PlayerName(named), chosenMode))
      )
    style = Style.button

  private val loadSave = new Button("Load game..."):
    onAction = _ =>
      picked("Open a saved game").foreach(path => chosen(Command.LoadSave(path, PlayerName(named))))
    style = Style.button

  private val bonuses = new HBox:
    alignment = Pos.Center
    spacing = SpacedBy
    children = BonusEffect.values.toSeq.map(effect =>
      new ImageView(SpriteImages.of(Sprite.Bonus(effect))):
        fitWidth = BonusSize
        preserveRatio = true
    )

  private val logo = new ImageView(Image(getClass.getResourceAsStream(Logo))):
    viewport = LogoDrawnOn
    fitWidth = LogoWidth
    preserveRatio = true

  mazes.selectionModel().selectFirst()
  player.text.onChange((_, _, _) => refuseEmptyName())
  refuseEmptyName()

  /** What to put on a scene to choose a game. */
  val node: Parent = new VBox:
    alignment = Pos.TopCenter
    spacing = SpacedBy
    padding = Insets(SpacedBy * 3, SpacedBy, SpacedBy, SpacedBy)
    style = Style.menu
    children = Seq(
      logo,
      bonuses,
      player,
      modes,
      mazes,
      new HBox:
        alignment = Pos.Center
        spacing = SpacedBy
        children = Seq(play, standings, loadMap, loadSave)
    )

  private def named: String = player.text().trim

  private def chosenMode: ModeChoice = modes.value()

  private def chosenMap: Option[MapName] =
    Option(mazes.selectionModel().getSelectedItem).map(MapName.apply)

  private def refuseEmptyName(): Unit =
    Seq(play, loadMap, loadSave).foreach(_.disable = named.isEmpty)

  private def showStandings(): Unit = chosenMap.foreach(maze =>
    val mode = LeaderboardMode.of(chosenMode)
    LeaderboardWindow.open(
      offered,
      LeaderboardSelection(maze, mode),
      bestOn,
      node.scene().window()
    )
  )

  private def picked(asked: String): Option[Path] =
    val chooser = new FileChooser:
      title = asked
    Option(chooser.showOpenDialog(node.scene().window())).map(_.toPath)

object MenuScreen:
  private val SpacedBy = 12.0
  private val FieldWidth = 320.0
  private val ListHeight = 140.0
  private val LogoWidth = 620.0
  private val BonusSize = 72.0
  private val Logo = "/logo.png"

  // The drawn part of logo.png
  private val LogoDrawnOn = Rectangle2D(142, 516, 1719, 953)

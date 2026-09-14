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

import java.io.IOException
import java.nio.file.{Files, Path}

/**
  * The screen a game is started from: who is playing, on which maze, and how others did on it.
  *
  * It emits [[Command]] values through `handleCommand`; it never accesses application state directly.
  */
final class MenuScreen(
    availableMazes: Seq[MapName],
    leaderboardFor: (MapName, LeaderboardMode) => Leaderboard,
    initialPlayerName: Option[PlayerName],
    savesFolder: Path,
    handleCommand: Command => Unit
):

  import MenuScreen.*

  private val playerNameField = new TextField:
    promptText = s"Your name (max $MaxNameLength)"
    maxWidth = FieldWidth
    text = initialPlayerName.fold("")(_.value)

  private val gameModeSelector = new ComboBox[ModeChoice](ObservableBuffer.from(ModeChoice.values.toSeq)):
    maxWidth = FieldWidth
    prefWidth = FieldWidth
    value = ModeChoice.Normal

  private val mazeSelector = new ListView[String](ObservableBuffer.from(availableMazes.map(_.value))):
    maxWidth = FieldWidth
    maxHeight = ListHeight

  private val leaderboardButton = new Button("View leaderboard"):
    style = Style.button
    onAction = _ => showStandings()

  private val playButton = new Button("Play"):
    onAction = _ =>
      selectedMaze.foreach(maze => handleCommand(Command.StartGame(maze, PlayerName(playerName), selectedMode)))
    style = Style.button
    defaultButton = true

  private val loadMapButton = new Button("Load map..."):
    onAction = _ =>
      selectedFile("Open a maze").foreach(path =>
        handleCommand(Command.LoadMap(path, PlayerName(playerName), selectedMode))
      )
    style = Style.button

  private val loadSaveButton = new Button("Load game..."):
    onAction = _ =>
      selectedFile("Open a saved game", Some(savesFolder)).foreach(path =>
        handleCommand(Command.LoadSave(path, PlayerName(playerName)))
      )
    style = Style.button

  private val bonusPreview = new HBox:
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

  mazeSelector.selectionModel().selectFirst()
  playerNameField.text.onChange((_, _, entered) =>
    val limited = limitedName(entered)
    if entered != limited then playerNameField.text = limited else updateButtonsForPlayerName()
  )
  updateButtonsForPlayerName()

  /** What to put on a scene to choose a game. */
  val node: Parent = new VBox:
    alignment = Pos.TopCenter
    spacing = SpacedBy
    padding = Insets(SpacedBy * 3, SpacedBy, SpacedBy, SpacedBy)
    style = Style.menu
    children = Seq(
      logo,
      bonusPreview,
      playerNameField,
      gameModeSelector,
      mazeSelector,
      new HBox:
        alignment = Pos.Center
        spacing = SpacedBy
        children = Seq(playButton, leaderboardButton, loadMapButton, loadSaveButton)
    )

  private def playerName: String = playerNameField.text().trim

  private def selectedMode: ModeChoice = gameModeSelector.value()

  private def selectedMaze: Option[MapName] =
    Option(mazeSelector.selectionModel().getSelectedItem).map(MapName.apply)

  private def updateButtonsForPlayerName(): Unit =
    Seq(playButton, loadMapButton, loadSaveButton).foreach(_.disable = playerName.isEmpty)

  private def showStandings(): Unit = selectedMaze.foreach(maze =>
    val mode = LeaderboardMode.of(selectedMode)
    LeaderboardWindow.open(
      availableMazes,
      LeaderboardSelection(maze, mode),
      leaderboardFor,
      node.scene().window()
    )
  )

  private def selectedFile(dialogTitle: String, initialFolder: Option[Path] = None): Option[Path] =
    val chooser = new FileChooser:
      title = dialogTitle
    initialFolder.flatMap(availableFolder).foreach(folder => chooser.initialDirectory = folder.toFile)
    Option(chooser.showOpenDialog(node.scene().window())).map(_.toPath)

  private def availableFolder(folder: Path): Option[Path] =
    try Some(Files.createDirectories(folder))
    catch case _: IOException => None

object MenuScreen:
  private val SpacedBy = 12.0
  private val FieldWidth = 320.0
  private val ListHeight = 140.0
  private val LogoWidth = 620.0
  private val BonusSize = 72.0
  private val MaxNameLength = 24
  private val Logo = "/logo.png"

  private[view] def limitedName(name: String): String = name.take(MaxNameLength)

  // The drawn part of logo.png
  private val LogoDrawnOn = Rectangle2D(142, 516, 1719, 953)

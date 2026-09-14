# Scala-Man

[![Scala Tests](https://github.com/PPS-25/PPS-25-scala-man/actions/workflows/ci.yml/badge.svg)](https://github.com/PPS-25/PPS-25-scala-man/actions/workflows/ci.yml)
[![Project Delivery](https://github.com/PPS-25/PPS-25-scala-man/actions/workflows/cd.yml/badge.svg)](https://github.com/PPS-25/PPS-25-scala-man/actions/workflows/cd.yml)

> A single-player arcade maze game, inspired by Pac-Man and written in Scala 3.

Guide Scala-Man through a maze, collect every standard item, use temporary bonuses, and stay clear
of enemies with distinct tactics. The game combines familiar arcade rules with custom ASCII maps,
multiple game modes, saved games, and map-specific leaderboards.

## Play

Scala-Man requires **JDK 21**. From a checkout of this repository:

```bash
sbt run
```

To create a self-contained executable JAR:

```bash
sbt assembly
java -jar target/scala-3.3.5/scala-man-0.1.0-SNAPSHOT-fat.jar
```

Published builds, when available, can be found under [Releases](../../releases).

### Controls

| Action | Control |
| --- | --- |
| Move | Arrow keys or `W`, `A`, `S`, `D` |
| Pause or resume | `Esc` or the Pause button |
| Restart, save, or return to the menu | Pause overlay |

## Gameplay

| Area | What it offers |
| --- | --- |
| **Objectives** | Clear all standard items in Classic and Timed, or survive as long as possible in Survival. |
| **Enemies** | Strategy-based enemies pursue the player directly or anticipate their route. |
| **Bonuses** | Invulnerability lets the player defeat enemies temporarily; Slowdown halves enemy speed. |
| **Maps** | Choose a supplied maze or load a validated ASCII map created by the player. |
| **Competition** | Scores are ranked separately for each map and game mode. |
| **Persistence** | Saved games preserve the level state and embed a copy of the selected map. |

Classic rewards collected items, bonuses, defeated enemies, and remaining lives. Timed ranks runs
by time left at completion, while Survival ranks difficulty waves survived.

## Maps and saved games

Select **Load map...** in the menu to try an external UTF-8 map file. Valid maps are copied to the
user data folder and remain selectable after the source file is moved or deleted:

```text
~/.scala-man/maps
```

Saved games, player details, and leaderboards are stored under `~/.scala-man` as well. On Windows,
`~` normally corresponds to `C:\Users\<username>`.

The [map-format guide](docs/map-format.md) explains every symbol and validation rule needed to
create a playable map. See also:

- [Game modes](docs/game-modes.md)
- [Enemy AI strategies](docs/enemy-ai-strategies.md)
- [Leaderboards](docs/leaderboards.md)
- [Saved games](docs/save-games.md)
- [Map generation](docs/map-generation.md)

## Development

The project uses sbt, ScalaTest, scalafmt, and GitHub Actions.

```bash
sbt compile test scalafmtCheckAll
sbt assembly
```

Useful repository entry points:

- [Application source](src/main/scala)
- [Automated tests](src/test/scala)
- [Documentation](docs)
- [Final report source](report/README.md)
- [Pre-push and pre-PR checklist](docs/pre-push-pre-pr.md)
- [Continuous integration workflow](.github/workflows/ci.yml)
- [Delivery workflow](.github/workflows/cd.yml)

## Authors

- [Matilde D'Antino](https://github.com/matidan01)
- [Gaia Mazzoni](https://github.com/GaiaMazzoni)
- [Alex Santini](https://github.com/AlexSantini10)

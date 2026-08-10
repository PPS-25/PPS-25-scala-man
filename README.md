# scala-man

> A Pac-Man-inspired arcade game written in Scala 3.

`scala-man` is a maze-based game where the player moves through the map, collects items, avoids enemies, and tries to clear the level before losing all lives.

## Authors

- [Matilde D'Antino](https://github.com/matidan01)
- [Gaia Mazzoni](https://github.com/GaiaMazzoni)
- [Alex Santini](https://github.com/AlexSantini10)

## Highlights

- arcade gameplay set in a maze
- multiple enemy behaviors, including chase and anticipation
- temporary bonuses such as invulnerability and enemy slowdown
- ASCII-based maze loading and validation
- clear win and lose conditions

## Project structure

- game code: [`src/main/scala`](src/main/scala)
- unit tests: [`src/test/scala`](src/test/scala)
- integration tests: [`src/it/scala`](src/it/scala)
- documentation: [`docs`](docs)
- report source: [`report/README.md`](report/README.md)

## Quick links

- project spec: [`docs/project-spec.md`](docs/project-spec.md)
- development rules: [`docs/professor-rules.md`](docs/professor-rules.md)
- pre-push / pre-PR checklist: [`docs/pre-push-pre-pr.md`](docs/pre-push-pre-pr.md)
- build definition: [`build.sbt`](build.sbt)
- CI workflow: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)
- delivery workflow: [`.github/workflows/cd.yml`](.github/workflows/cd.yml)

## Development

```bash
sbt compile test scalafmtCheckAll
sbt assembly
```

The assembled executable JAR is produced by `sbt assembly`.

## Project status

The project is under active development.

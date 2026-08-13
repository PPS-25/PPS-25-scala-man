# Pre-push and pre-PR checks

Checklist for what to verify before publishing changes.

## Before push

1. Format the code before checking it:
   - `sbt scalafmtAll`
2. Run the basic local checks:
   - `sbt compile test scalafmtCheckAll`
3. If you changed packaging, also verify the assembly:
   - `sbt assembly`
4. Check that there are no generated files or unwanted diff changes.

## Before PR

1. Re-run the local checks if you changed code after the last push.
2. If the change affects the release, update the related documentation in [`docs`](../docs).

## Note

The project version is defined in [`build.sbt`](../build.sbt). The GitHub release published by CD uses the fixed `latest` tag, so it is not an automatic SemVer version of the software.

# CleanMac

Kotlin + Compose Desktop macOS app that frees disk space (see README.md). Source in `src/main/kotlin/app/cleanmac/`, tests in `src/test/kotlin/app/cleanmac/`.

## Commands
- `./gradlew test` — run tests
- `./gradlew run` — launch app
- `./gradlew packageDmg` — build installer

## Rules for every change
1. **Tests**: add or update tests for the change (`./gradlew test` must pass).
2. **Docs**: review README.md (and this file) and update anything affected.
3. **Version**: bump `version` in `build.gradle.kts` (SemVer, currently 0.x) and add an entry to CHANGELOG.md.
4. **Commits**: never offer to commit. Instead, finish by suggesting a commit message (Conventional Commits style).

## Safety
Deletion must go through `Cleaner` (allowlisted roots, no symlink following). Never widen `Scanner.allowedRoots` without a test and a README note.

## Build notes
Kotlin 2.2.x can't emit JDK 25 bytecode: Java/Kotlin target 21 is set explicitly in `build.gradle.kts`. macOS `packageVersion` can't start with 0, so `0.y.z` is packaged as `1.y.z`.

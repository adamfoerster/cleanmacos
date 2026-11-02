# CleanMac

Free, open-source alternative to CleanMyMac's space-freeing "Cleanup Manager" for macOS. Kotlin + Compose Desktop.

## Features
- Scans and lists reclaimable space: Xcode junk (Derived Data, Device Support, Archives, Products, Simulator caches), user caches, user logs, Trash, Mail downloads.
- Per-item checkboxes, "Smartly / All / None" selection, sort by size or name.
- Confirmation before deleting; reports freed space afterwards.
- Safe deletion: only inside an allowlist of folders, never follows symlinks.

Not included: simulator runtimes, system-level caches/logs (need admin), language files, universal binaries.

## Requirements
- macOS, JDK 21+ (tested up to 21; JDK 25 supported by the build, bytecode targets 21)
- Full Disk Access for the app/IDE running it, to scan Trash and Mail (the app shows a banner when missing)

## Usage
```bash
./gradlew run          # launch the app
./gradlew test         # run tests
./gradlew packageDmg   # build installer (unsigned: right-click > Open)
```
From Android Studio: Gradle panel > Tasks > compose desktop > `run`.

## Layout
`src/main/kotlin/app/cleanmac/`: `Model.kt` (data types), `Scanners.kt` (what to scan), `Fs.kt` (size + safe `Cleaner`), `State.kt` (UI state), `Main.kt` (Compose UI).

## Versioning
[SemVer](https://semver.org); version lives in `build.gradle.kts`. See [CHANGELOG.md](CHANGELOG.md). Since macOS packages need MAJOR >= 1, `0.y.z` is packaged as `1.y.z`.

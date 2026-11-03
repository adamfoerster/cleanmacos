# Changelog

Format: [Keep a Changelog](https://keepachangelog.com), versioning: [SemVer](https://semver.org).

## [0.3.0] - 2026-11-02
### Added
- Simulator runtimes appear under "Xcode Simulators Runtime", one selectable item per runtime, deleted with `xcrun simctl runtime delete`. Never pre-selected.

## [0.2.0] - 2026-11-02
### Added
- Expandable items with per-child selection (tri-state parent checkbox) for Derived Data, iOS/watchOS Device Support and Archives, so single devices/versions can be kept.

## [0.1.0] - 2026-11-02
### Added
- Initial Compose Desktop app with scanners for Xcode junk, user caches, user logs, Trash and Mail downloads.
- Safe `Cleaner` with allowlisted roots and symlink protection, with tests.
- Single "System Junk" view listing all subcategories, item selection, sorting and confirmation dialog.
- Build compatible with JDK 25 (Gradle 9.1, Kotlin 2.2.20, bytecode target 21).

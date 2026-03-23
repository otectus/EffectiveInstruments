# Changelog

All notable changes to Effective Instruments will be documented in this file.

## [1.2.0] - 2026-03-23

### Features
- **Instrument-specific auras:** Each instrument can have its own aura (or set of allowed auras) configured via `config/effective_instruments/instrument_auras.json`. The selector only shows auras allowed for the current instrument.
- **Per-instrument aura memory:** Manual aura overrides are remembered per-instrument within the session (forgotten on logout)
- **`/effectiveinstruments status [player]`** command to view aura state (selected aura, instrument, active status, buffed target count)
- CI workflow for automated builds via GitHub Actions
- Unit test scaffolding with JUnit 5

### Changes
- **Aura selection now clears when closing an instrument** (previously persisted across close/reopen). The instrument-specific default will auto-select on next open.
- Network protocol bumped to version 3 (clients and servers must use matching mod versions)
- `/effectiveinstruments reload` now also reloads instrument-aura mappings and reports mapping count

### Technical
- New `InstrumentOpenC2SPacket`: client sends instrument ID to server when opening an instrument screen
- New `SyncAuraSelectionS2CPacket`: server syncs auto-selected aura back to client
- `NoteActivityHandler` now captures instrument ID from note metadata as a fallback

## [1.1.0] - 2026-03-23

### Bug Fixes
- Fixed hover color mask in aura buttons destroying color information, making all hover states appear near-black
- Fixed `radius: 0` in aura JSON being treated as "use default" instead of a valid self-only radius
- Moved `screenClassAllowlist` from server config to client config so fallback screen detection works on dedicated servers
- Fixed aura deselect not clearing the affected targets tracking map, causing stale entries to accumulate over long play sessions
- Fixed redundant double lookup of aura registry on every aura tick
- Fixed particle spawn distance producing incorrect values at small radii (0 or 1)
- Fixed potential crash when rendering an aura button with an empty display name

### Improvements
- `overlayScale` and `compactMode` client config options now function as intended, controlling aura selector button sizing
- Added localization keys for command feedback and widget narration
- Improved accessibility: aura button narration now announces selected state and description

## [1.0.0] - Initial Release

### Features
- Aura buff system for Genshin Instruments
- 4 built-in aura presets: Soothing Hymn, Invigorating March, Guardian Chorus, Luminous Nocturne
- Fully data-driven aura definitions via JSON files
- Configurable targeting: self, other players, tamed pets, custom entity allowlist
- In-game aura selector overlay on instrument screens
- Colored note particle effects around active aura musicians
- Server and client configuration via TOML files
- `/effectiveinstruments reload` command for hot-reloading aura presets
- Compatible with Genshin Instruments and Even More Instruments

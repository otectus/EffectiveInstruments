# Changelog

All notable changes to Effective Instruments will be documented in this file.

## [1.2.1] - 2026-03-26

### Bug Fixes
- Fixed player logout not cleaning up aura effects on buffed targets in abrupt disconnection scenarios
- Fixed effect cleanup incorrectly stripping long-duration effects from other sources (e.g. beacons) that matched the aura's amplifier — now also checks remaining duration
- Fixed `onInstrumentClose` bypassing debug logging when clearing aura selection
- Fixed compact mode icon (16px) overflowing the 14px button — icon now scales to fit

### Security & Hardening
- Added 5-tick (~250ms) rate limiting on `SelectAuraC2SPacket` and `InstrumentOpenC2SPacket` to prevent packet spam
- Effect amplifiers in aura JSON files are now clamped to 0–4 (Level I–V) with a warning if exceeded
- Aura JSON files over 64KB are skipped with a warning to guard against maliciously large files
- Aura IDs derived from filenames are validated against `[a-z0-9_]+` — non-conforming files are skipped
- Suspicious instrument IDs from unknown namespaces are logged at WARN level

### Performance
- Pet entity allowlist is now cached statically instead of being re-parsed every tick interval per musician
- Tick handler now iterates only active musicians instead of all players in the level
- Enabled presets list is pre-computed on load instead of being rebuilt on every call

### Improvements
- Aura button tooltips now show actual effects with levels (e.g. "Speed I, Regeneration I")
- Aura selector buttons now wrap into multiple rows when total width exceeds 60% of screen width
- Replaced misleading `ConcurrentHashMap` with `HashMap` (all access is on the main server thread)
- Documented dimension-change cleanup limitation (effects expire naturally within 13 seconds)
- Updated test data from obsolete aura names to current v1.2.0 names
- Added link to INSTRUMENT_AURAS.md design reference in README
- Added server admin note in README about disabling Smoky Allure's Hero of the Village effect

## [1.2.0] - 2026-03-23

### Features
- **15 unique instrument auras** replacing the original 4 generic presets — each instrument now has its own thematic aura (Zephyr's Blessing, Warcry Cadence, Moonlit Passage, Smoky Allure, Ghost Flame, and more)
- **Instrument-specific aura filtering:** Each instrument can have its own set of allowed auras configured via `config/effective_instruments/instrument_auras.json`. The selector only shows auras allowed for the current instrument.
- **All EMI Note Block Instrument variants mapped** — all 16 variants (basedrum, bass, bell, bit, chime, etc.) now have assigned auras instead of showing all auras
- **Per-instrument aura memory:** Manual aura overrides are remembered per-instrument within the session (forgotten on logout)
- **`/effectiveinstruments status [player]`** command to view aura state (selected aura, instrument, active status, buffed target count)
- CI workflow for automated builds via GitHub Actions
- Unit test scaffolding with JUnit 5

### Changes
- **Aura selection now clears when closing an instrument** (previously persisted across close/reopen). The instrument-specific default will auto-select on next open.
- **Old aura presets replaced:** Soothing Hymn, Invigorating March, Guardian Chorus, and Luminous Nocturne have been replaced by 15 instrument-specific auras. Existing installs keep their old files (marker-based generation).
- Network protocol bumped to version 3 (clients and servers must use matching mod versions)
- `/effectiveinstruments reload` now also reloads instrument-aura mappings and reports mapping count
- Server-side validation: players can only select auras allowed for their current instrument

### Technical
- New `InstrumentOpenC2SPacket`: client sends instrument ID to server when opening an instrument screen
- New `SyncAuraSelectionS2CPacket`: server syncs auto-selected aura back to client
- `NoteActivityHandler` now captures instrument ID from note metadata as a fallback
- `InstrumentAuraMapping` supports both string shorthand and object form (with `default` + `allowed` list)

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

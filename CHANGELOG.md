# Changelog

All notable changes to Effective Instruments will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.4.8] - 2026-04-18

### Fixed
- **Duplicate offensive auras across instruments.** The effect-based
  synthesis fallback used for custom positive auras returned the first
  offensive with a matching primary effect — so every regen-primary
  positive resolved to `echoes_of_decay`, etc. Renamed to
  `findUnusedOffensiveByEffect(positive, taken)`; batched synthesis now
  tracks already-claimed offensive ids and skips them.
- Unique-assignment migration marker bumped `_v1_done` → `_v2_done` so
  installs with lingering duplicates re-run the fix once.

### Added
- **`offensiveTargeting.includeAllNonPets`** server config (default
  `true`). When true, offensive auras target every living entity in
  range except the musician, their own pets, and other players' pets —
  ignoring per-category knobs. Set to `false` for fine-grained control.
- **`/effectiveinstruments reset-mappings`** (OP) — deletes the mapping
  JSON + migration markers and regenerates from the canonical table.
- **Load-time uniqueness audit.** Warns if any offensive aura appears on
  more than one instrument after synthesis.
- `/effectiveinstruments diagnose` now shows `includeAllNonPets` plus
  every per-category offensive toggle in one line.

## [1.4.7] - 2026-04-18

### Fixed
- **Durability depletion blocked in creative.** The 1.4.6 hardcoded
  `player.getAbilities().instabuild` immunity made creative testing
  opaque. Replaced with server config `durability.creativeImmunity`
  (default `true`; flip to `false` to verify depletion in creative).
- **Offensive auras felt non-functional.** Three compounding issues:
  - `isActive` required `instrumentOpen=true`, so the aura deactivated
    the instant the screen closed (before users could observe effects).
    Removed the screen-open gate; recent notes alone keep it active.
  - Tick-based apply had up to 500 ms latency. Added
    `AuraManager.applyAuraNow(player)` called synchronously from
    `NoteActivityHandler` — effects land within ms of the first note.
  - Handler listened on abstract `InstrumentPlayedEvent<?>` only.
    Forge EventBus dispatch to abstract-parent listeners is fragile;
    now subscribes to concrete `NoteSoundPlayedEvent` +
    `HeldNoteSoundPlayedEvent`.
- `activeMusicians` is also populated on `onNotePlayed` (defensive
  against a missed `InstrumentOpenStateChangedEvent`).

### Added
- **`/effectiveinstruments diagnose`** — dumps every gate in the aura
  pipeline (held items + tracked state, current selection, `isActive`,
  master toggles, activation thresholds). Answers "why isn't my aura
  firing?" in one command.

## [1.4.6] - 2026-04-18

### Fixed
- **IM selector icons render crisply.** 1.4.5 used the 9-arg `blit`
  overload with mismatched source/dest sizes, producing wrap/clip
  artifacts. 1.4.6 uses the 10-arg scaling overload (same as
  `AuraButtonWidget`) and drops `IM_ICON_SIZE` to 16 for 1:1 scaling.
- **Broken-message + low-durability throttles now per-player.** 1.4.5
  used static `long`s shared across all players; one musician's
  broken-spam suppressed another's warning. Now `Map<UUID, Long>` with
  cleanup on logout.

### Added
- **Mobile-tier particle spawning.** Extracted
  `AuraManager.spawnAuraNotes(player, aura, radius)` and wired it into
  `ImmersiveMelodiesAuraHandler.tickPlayer` — musical notes now float
  around the player while IM is playing.
- **Creative-mode durability immunity** (matches vanilla tool
  behavior). *Reworked into a config flag in 1.4.7.*
- **Low-durability warning at ≤10% remaining.** Gold action-bar message
  throttled once per second.
- **Durability registry-lookup cache.** `InstrumentDurability` caches
  `Item → Entry` in a `ConcurrentHashMap`, invalidated on config
  reload. Drops an `ITEMS.getKey` from every slot-render frame.
- **Aura-hint in instrument tooltip.** Dark-gray italic hint surfaces
  the selector to players who pick up an instrument without docs.
- **Action-bar hint** when an IM screen opens without a held IM
  instrument — explains the silent-skip path.
- **Preset existence-gates** extended to offensive + mobile writers,
  so user-edited preset JSONs survive marker bumps uniformly.
- **Removed dead reflection.** `AuraSelectorWidget.reattachTo` +
  `onScreenRenderPre` were load-bearing in 1.4.4 for the widget-based
  IM path; obsolete since 1.4.5 made IM render-event based.

## [1.4.5] - 2026-04-18

### Fixed
- **IM overlay is now render-event based.** 1.4.4's widget-reattach
  mechanism wasn't enough — on some installs the icons never appeared.
  Root causes: IM rebuilds its widget list aggressively, and when the
  held instrument has no mobile mapping the injector bailed silently.
  1.4.5 draws icons directly in `ScreenEvent.Render.Post`, handles
  clicks via `ScreenEvent.MouseButtonPressed.Pre`, and falls back to
  showing all enabled mobile presets when the mapping is missing.

### Added
- **Durability bar on every instrument slot.** New
  `InstrumentDurabilityBarDecorator` (client-only) registers an
  `IItemDecorator` via `RegisterItemDecorationsEvent` for every item in
  `genshinstrument`, `evenmoreinstruments`, `immersive_melodies`
  namespaces. Vanilla-style 2×13px bar with green→yellow→red gradient.
- **Base durability 4× higher.** Per-instrument defaults scaled
  200→800, 300→1200, 400→1600. Global fallback `defaultMax` 300→1200.
  Marker `_v2` forces one-time regen.

## [1.4.4] - 2026-04-18

### Fixed
- **Icons flash-then-vanish on IM screens.** Added
  `ScreenEvent.Render.Pre` handler that idempotently re-adds the
  selector widget when IM wipes its children list.
- **GI offensive aura selections silently dropped.**
  `SelectAuraC2SPacket` stationary handler now accepts the exact
  shipped polarity counterpart of the instrument's default even when
  the allowed list predates 1.4.x.
- **Offensive effects invisible on mobs.** Positive auras keep
  `ambient=true` (subtle friendly-target particles); offensive auras
  now set `ambient=false` so hostile mobs show normal particle plumes.
- **Offensive-effect cleanup on aura switch.** `AuraApplicator.clear`
  is now polarity-aware — strips `ambient=false` effects too.
- **`earth_tremor` ⟷ `fathomless_pull` tuple duplicate** from 1.4.x.

### Added
- **1-to-1 unique auras.** Positive-stationary table expanded 15 → 31
  (sixteen new ids: skyward_zephyr, rumbling_anthem, thunderous_cadence,
  drumline_vigor, artisan_tempo, chiming_revival, starlit_grace,
  fleetfoot_lilt, troubadour_march, craftwork_rondo, ironwright_anthem,
  pasture_serenade, hearthlight_drone, pixel_pulse, wayfinders_reel,
  bellwether_toll). Offensive-stationary table expanded 15 → 31
  (sixteen mirrored ids). All 32 new icons generated via
  `tools/gen_aura_icons.py`.
- **One-shot 1-to-1 migration.** Marker
  `.instrument_unique_assignment_v1_done` reassigns legacy duplicates
  in the user's mapping while preserving custom entries.
- **Uniqueness guard at `AuraRegistry.load()`.** Warn-level log lists
  any preset ids that share a `(polarity, effects, amplifiers)` tuple.
- **Atomic mapping-file writes** via `InstrumentAuraMapping.writeAtomically`
  (writes to `.tmp` then `Files.move` with `ATOMIC_MOVE`).

## [1.4.3] - 2026-04-18

### Fixed
- Icons gracefully fall back on missing textures —
  `AuraButtonWidget` pre-checks the texture via the resource manager
  before blit.
- **Effect-based offensive pairing for custom user auras.**
  `InstrumentAuraMapping.findOffensiveByEffect` maps a custom
  positive's primary effect through the inverse table and finds a
  shipped offensive whose first effect matches.
- **Immersive Melodies GUI integration.** The aura selector now
  appears on both IM screens (selection + free-play). Retired the old
  `B` keybind + standalone `MobileAuraPickerScreen`.
- **Free-play mobile auras apply.** IM's `playing=true` NBT flag isn't
  set during free-play; `InstrumentOpenC2SPacket` grew
  `mobileTier`/`close` fields so the server can activate the aura when
  the IM screen opens. Durability is still only charged on playing=true
  (browsing the melody list is free).

## [1.4.2] - 2026-04-18

### Fixed
- **Icons regenerated.** 1.3.x-era positive icon PNGs were 224/256 px
  transparent. Replaced with procedural flat-color + effect-specific
  glyph icons via `tools/gen_aura_icons.py` (single source of truth).
- **Mobile positive presets now have icons.** Mobile marker bumped
  `_v2`; `AuraJsonLoader.writeMobileDefault` emits `icon`/`iconSelected`
  + `showInSelector=true`.
- **Loud diagnostics on synthesis failure.**
  `synthesizeMissingOffensiveAllowed` logs the exact config file path
  the user should delete to force regeneration.

## [1.4.1] - 2026-04-18

### Fixed
- **Icon render fix** — `AuraButtonWidget.blit` passes fixed 16×16
  source dimensions (no more missing-texture squares at compact scale)
  and falls back to the letter renderer on any draw failure.
- **Self-healing mappings.** `InstrumentAuraMapping.load` and
  `MobileInstrumentAuraMapping.load` synthesize missing offensive
  `allowed` entries in memory and opportunistically rewrite the JSON.
  `AuraJsonLoader.healMissingOffensivePresets` regenerates any
  offensive preset file that vanished from disk.

### Added
- **Offensive icons shipped.** 26 procedural PNGs generated by
  `tools/gen_offensive_icons.py`. Offensive-defaults marker bumped
  `_v2` so upgraded installs regenerate their JSONs once.
- **Targeting contract tightened.** Positive auras *always* include the
  musician and their own pets; offensive auras *never* do. Everything
  else is per-polarity config. New `positiveTargeting` /
  `offensiveTargeting` category toggles. Legacy `targeting.allowSelfBuff`
  + `targeting.includeTamedPets` deprecated.
- **`EntityCategory`** — centralized classifier.
  `AuraApplicator.gatherTargets` replaced a two-branch implementation
  with a single-loop classifier that buckets candidates and emits in
  priority order.

## [1.4.0] - 2026-04-18

### Added
- **Offensive auras.** Every instrument gains a second "negative
  polarity" preset (Wither, Poison, Slowness, etc.). Target set
  inverts: musician + pets spared, everything else in range debuffed.
  Selector shows offensive presets with a red border.
  Master switch: `offensiveTargeting.enabled`.
- **Instrument durability.** Per-instrument NBT health under tag key
  `EIDurability`. Fresh stacks assume full durability (lazy init).
  Damage per note / per mobile pulse is config-driven; offensive auras
  multiply the cost. Config file:
  `config/effective_instruments/instrument_durability.json`.
- **Broken-state gate.** At 0 durability,
  `InstrumentPlayedEvent.setCanceled(true)` — no sound, no aura.
  Open-events still pass but warn via chat.
- **Anvil repair.** Two paths: combine two damaged copies (+12% max
  bonus, vanilla-style) or consume the configured repair material for
  `repairPerUnit` durability each.
- **Admin subcommand.** `/effectiveinstruments durability {get|set <n>|repair}`.

## [1.3.0] - 2026-04-15

### Build & Distribution
- **Reproducible builds:** dependencies now resolve from Curse Maven
  (`cursemaven.com`) instead of requiring local jars in `Dependencies/`.
  Project + file IDs live in `gradle.properties` so version bumps touch
  one file. A fresh clone builds with `./gradlew build` — no manual setup.
- `gradle/wrapper/gradle-wrapper.jar` is now committed, so `./gradlew`
  works on a clean clone.
- Added `LICENSE` file at repo root (MIT), aligning the three previous
  sources of truth (gradle.properties, mods.toml, repo root).
- CI hardening: workflow now declares `permissions: contents: read`,
  uploads test reports on failure with 7-day retention, and sets
  14-day retention on mod jar artifacts.
- New `.github/workflows/dependency-submission.yml` publishes the Gradle
  dependency graph to GitHub Insights for supply-chain visibility.

### Data Schema
- Aura preset JSON and `instrument_auras.json` now accept an optional
  `schemaVersion` field (current: 1). Files with a schema newer than the
  running mod are rejected with a clear log message. Missing field is
  treated as v1, so existing configs keep working untouched.
- Added `docs/schemas/aura.schema.json` and
  `docs/schemas/instrument_auras.schema.json` (JSON Schema Draft-07)
  for editor autocomplete and validation.

### Security & Trust Boundary
- **Server-authoritative instrument-open state:** `InstrumentOpenC2SPacket`
  no longer sets the authoritative open flag. That transition is now
  exclusively driven by the server-side `InstrumentOpenStateChangedEvent`
  handler. A spoofed client packet cannot pre-apply aura effects, because
  the aura tick still requires the authoritative flag.
- Rate limiting for the open and aura-select packets now tracks
  cooldowns independently (`lastOpenPacketTick` vs `lastSelectionTick`),
  so the two packet types cannot starve each other.

### Gameplay / Modpack Features
- **`effectOverwritePolicy` server config**: controls how aura effects
  interact with pre-existing effects on targets. Choices:
  `NEVER_OVERWRITE`, `STRONGER_ONLY`, `REFRESH_TIES` (default — matches
  pre-1.3.0 behavior), `ALWAYS`.
- **Activation thresholds**: new `noteThresholdMin` + `noteThresholdWindowTicks`
  server config keys. An aura only activates when the player has played
  at least N notes within the sliding window. Default is 1, preserving
  legacy behavior; pack authors can raise it to 3+ to prevent
  "hold one note" exploits.
- **`maxTargetsPerTick` server config**: hard cap on entities buffed per
  musician per tick (default 32). Prevents performance cliffs in crowded
  farms. Targets beyond the cap are dropped in insertion order
  (self → players → pets).
- **`debugMode` server config**: when true, logs per-tick diagnostics
  (active musicians, scanned targets, elapsed microseconds).

### Dual-Tier System (Immersive Melodies compat)
- **Mobile tier**: passive, server-authoritative buff tier that activates
  when a player holds a playing Immersive Melodies instrument. No hard
  dependency — when Immersive Melodies is absent the compat layer is a
  silent no-op. Stationary tier (Genshin Instruments / EMI screen play)
  is unchanged and wins precedence when both would apply to the same
  player (configurable via `suppressWhenStationaryActive`).
- New `mobileTier` server config group: `enabled`, `pulseIntervalTicks`
  (default 20), `lingerTicks` (60), `defaultRadius` (8),
  `maxTargetsPerTick` (16), `allowSelfBuff`, `includeOtherPlayers`,
  `includeTamedPets` (off by default), `suppressWhenStationaryActive`.
- New mobile instrument mapping file
  `config/effective_instruments/mobile_instrument_auras.json` with
  defaults for all 11 Immersive Melodies instruments. Reloadable via
  `/effectiveinstruments reload`.
- New aura preset fields `tiers` (array: `["stationary"]`, `["mobile"]`,
  or both) and `showInSelector` (bool). Both are optional and default to
  pre-1.3.0 behavior, so existing JSONs need no edits. Mobile-only
  presets never leak into the stationary selector UI.
- Bundled 11 mobile-tier default presets (one per IM instrument),
  each single-effect and short-duration for deliberate balance.
- New migration markers `.mobile_aura_defaults_generated` and
  `.mobile_instrument_defaults_generated` ensure upgraded installs
  receive mobile defaults without clobbering stationary customizations.
- **Known limitation**: mobile buffs fire for autoplay / selected-melody
  playback only. Immersive Melodies free-play keyboard/MIDI mode does
  not write the `playing=true` NBT flag this mod checks. Scheduled for
  revisit in 1.3.1 if an upstream IM hook lands.

### Refactor — AuraApplicator
- Extracted `AuraApplicator` + `TargetingProfile` from `AuraManager`.
  Both tiers now share the same strongest-wins effect logic, target
  gathering, and tracked-target cleanup. Zero behavior change for the
  stationary tier (`OverwritePolicy` semantics, pet allow-list, and
  `maxTargetsPerTick` cap all preserved).
- `AuraManager.isActive(UUID, long)` is now a public thin wrapper used
  by the mobile tier's suppression check.

### API
- New `com.crims.effectiveinstruments.api.EffectiveInstrumentsAPI` facade.
  Third-party instrument mods can now drive aura lifecycle events without
  importing internal classes:
  `notifyInstrumentOpen`, `notifyInstrumentIdReceived`,
  `notifyInstrumentClose`, `notifyNotePlayed`.
- Extended the API with `isImmersiveMelodiesCompatActive()` and
  `getMobileState(ServerPlayer)` returning an immutable `MobileState`
  snapshot for HUD addons and telemetry.

### Accessibility
- New client config `reducedMotion`: dampens aura particle drift and
  pulse amplitude for players sensitive to rapid visual motion.

### Commands
- New `/effectiveinstruments help` subcommand printing config location
  and available commands.
- `/effectiveinstruments status <player>` now reports mobile-tier state
  (aura id, instrument id, active flag, buffed-target count) whenever
  Immersive Melodies compat is active.
- `/effectiveinstruments reload` now reports the mobile mapping count
  and compat activation state on success.

### Tests
- Added `OverwritePolicyTest` (5 cases), `AuraSchemaGateTest` (7 cases),
  `AuraTierJsonTest` (14 cases), `AuraApplicatorBehaviorTest` (12 cases),
  and `MobileInstrumentAuraMappingJsonTest` (10 cases). Total unit test
  count: ~55.

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

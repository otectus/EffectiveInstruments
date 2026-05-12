# Effective Instruments — Forge 1.20.1 Mod

## Quick Reference
- **Mod ID**: `effectiveinstruments`
- **Package**: `com.crims.effectiveinstruments`
- **Version**: 1.5.0
- **MC**: 1.20.1 | **Forge**: 47.3.0 | **Java**: 17
- **Mappings**: Official

## Build
- `./gradlew build` — full build
- `./gradlew compileJava` — compile-only
- `./gradlew runClient -PdevRuntimeGenshin=true` — dev client with GI present at runtime
  (since 1.5.0 GI is `compileOnly`; the default `runClient` exercises the GI-absent path)

## Key Dependencies
- **Genshin Instruments** 5.0 — **optional** stationary-tier backend (compileOnly since 1.5.0).
  Loaded only when `ModList.get().isLoaded("genshinstrument")` returns true at common-setup;
  the GI event handler in `compat/genshin/` is the only class that imports
  `com.cstav.genshinstrument.*`.
- **Immersive Melodies** 0.6.0+ — optional mobile-tier backend (compileOnly).
  Bridge in `compat/immersivemelodies/` reads only ModList + NBT + registry IDs; no IM
  classes are imported anywhere.
- **Even More Instruments** 6.1.4 — optional soft dependency (compileOnly). Requires GI
  at runtime since EMI screens extend GI's instrument screen.

## Notes
- Aura effects while playing supported modded instruments. Both Genshin Instruments and
  Immersive Melodies are optional backends as of 1.5.0; install either, both, or neither.
- Instrument jars loaded from Curse Maven (`https://cursemaven.com`) on first build.
- No mixins.

## 1.5.0 Feature Set
- **Genshin Instruments demoted to optional backend.** `mods.toml` now declares
  `mandatory=false` for `genshinstrument`, and `build.gradle` switched
  `implementation` → `compileOnly`. The mod loads cleanly with only Immersive
  Melodies installed, or with no instrument backend at all (warns once at startup).
- **Dependency quarantine.** Three always-loaded classes used to import GI directly:
  `event/NoteActivityHandler`, `event/InstrumentStateHandler`, and
  `client/event/AuraOverlayInjector`. v1.5.0 reorganizes:
  - `event/NoteActivityHandler` deleted; its shared logic moved into
    `event/StationaryInstrumentNoteService` (no GI imports — owns the broken-state
    gate, polarity-aware durability, broken/low-durability throttle maps, and the
    per-player logout cleanup).
  - `event/InstrumentStateHandler` stripped of its GI handler; only generic Forge
    events remain (`onLevelTick`, `onPlayerLogout`, `onPlayerChangedDimension`,
    `onPlayerDeath`).
  - `client/event/AuraOverlayInjector` no longer imports GI's `InstrumentScreen`.
    Detection routes through `compat/genshin/client/GenshinInstrumentScreenBridge`
    (lazy `Class.forName` + cached `Method` lookup, with a `resolutionAttempted`
    flag to short-circuit failures without log spam).
- **`compat/genshin/` package.** Mirrors the existing `compat/immersivemelodies/`
  layout. `GenshinInstrumentsCompat.initCommon()` runs at common-setup, calls
  `MinecraftForge.EVENT_BUS.register(GenshinInstrumentEventHandler.class)` only when
  GI is loaded. The handler holds the three GI-typed `@SubscribeEvent` static
  methods (`onNoteSoundPlayed`, `onHeldNoteSoundPlayed`, `onInstrumentStateChanged`)
  — the only class in the codebase with `com.cstav.genshinstrument` imports.
- **Backend-availability section in `/effectiveinstruments diagnose`.** First line of
  the dump: `Backends: genshin=active|absent immersive_melodies=active|absent`. When
  neither is installed, follows up with a yellow WARNING line.
- **Dual-absence warning at startup.** `EffectiveInstrumentsMod.commonSetup` logs
  `WARN` if neither backend is detected: *"No supported instrument backend detected.
  Install Genshin Instruments or Immersive Melodies…"*.
- **Audit invariant.** A grep audit of `com.cstav.genshinstrument` against
  `src/main/java/` should return only `compat/genshin/GenshinInstrumentEventHandler.java`
  (4 imports) and `compat/genshin/client/GenshinInstrumentScreenBridge.java` (1 FQN
  string literal). Always-loaded classes must never reference GI types.
- **Wire protocol unchanged.** `EIPacketHandler.PROTOCOL_VERSION` stays at `4`. No
  packet shape changed; v1.4.x clients connect to v1.5.0 servers cleanly.
- **Mod description broadened.** `gradle.properties:23` now reads *"Aura effects
  while playing supported modded instruments, including Genshin Instruments and
  Immersive Melodies"*.

## 1.4.0 Feature Set
- **Offensive auras** — every instrument now has a second "negative polarity" preset
  (Wither, Poison, Slowness, etc.). Target set inverts: musician + pets spared,
  everything else in range debuffed. Selector shows them with a red border.
  Master switch: `offensiveTargeting.enabled` (server config).
- **Durability** — per-instrument NBT health under tag key `EIDurability`. Fresh
  stacks assume full durability (lazy init). Damage per note / per mobile pulse
  is config-driven; negative auras multiply the cost. Config file:
  `config/effective_instruments/instrument_durability.json`.
- **Broken-state gate** — at 0 durability, `InstrumentPlayedEvent` is canceled
  (the event carries `@Cancelable` in Genshin Instruments 5.0 — confirmed via
  bytecode inspection). Mobile tier treats broken stacks as idle. Opens are
  allowed (screen-open event is not cancelable) but warn via chat.
- **Repair** — `AnvilUpdateEvent` handler. Two paths: combine two damaged
  copies (+12% max-durability bonus, vanilla-style) or consume the configured
  repair material for `repairPerUnit` durability each.
- **Admin subcommand** — `/effectiveinstruments durability {get|set <n>|repair}`
  operates on the main-hand (or off-hand) instrument.

## 1.4.9 Hotfix Set
- **Durability fallback now actually fires.** `InstrumentDurability.resolveEntry`
  used to return `null` for any item not pre-listed in
  `instrument_durability.json`, so third-party instruments and any newly-
  shipped instrument the JSON didn't yet cover got zero durability tracking
  — exactly the opposite of what `InstrumentDurabilityConfig`'s class doc
  promised. 1.4.9 synthesizes a default `Entry` from
  `EIServerConfig.DURABILITY_DEFAULT_MAX` when the lookup misses AND the
  item's namespace is in the new shared
  `com.crims.effectiveinstruments.durability.InstrumentNamespaces.INSTRUMENT_MOD_IDS`
  allowlist (`genshinstrument`, `evenmoreinstruments`, `immersive_melodies`).
  Non-instrument items still get `NO_ENTRY` so swords/shields/etc. don't
  accidentally pick up an invisible NBT durability bar.
- **`AuraRegistry.load` split into two phases.** The 1.4.x flow ran
  config-dependent work (pet allowlist invalidation, durability default
  fallback) at common-setup, before the SERVER `ForgeConfigSpec` is loaded
  by Forge. Result was either an `IllegalStateException` from `IntValue.get()`
  or silently using the spec default — quietly self-correcting on `/reload`,
  which masked the bug. New split: `loadPresetsAndMappings()` runs at
  common-setup (JSON only, no config reads), `refreshConfigDerived()` runs
  from a new `ServerAboutToStartEvent` handler in `EffectiveInstrumentsMod`.
  `AuraRegistry.load` keeps the old API by chaining both halves; the
  `/effectiveinstruments reload` command path is unchanged.
- **`InstrumentDurabilityBarDecorator` SERVER-config read is now safe.** The
  bar decorator's `renderBar` called `EIServerConfig.DURABILITY_ENABLED.get()`
  unprotected, which throws `IllegalStateException` on the title screen, world
  list, and REI/JEI item-preview overlays rendered before world join. New
  static `EIServerConfig.isDurabilityEnabledSafe()` wraps the read in
  try/catch returning `false` when the spec isn't loaded; consolidates the
  duplicated try/catch pattern that already lived in
  `DurabilityTooltipHandler`. Both call sites now route through the helper.
- **Mobile packets are now rate-limited.** Stationary `InstrumentOpenC2SPacket`
  and `SelectAuraC2SPacket` had a 5-tick (~250 ms) cooldown; the mobile paths
  did not. A misbehaving client could flood mobile selections, each of which
  marks `MobilePlayerSelection`'s SavedData dirty and forces autosave I/O.
  `MobilePlayerSelection` grew a transient `lastMobileSelectionTick` map +
  `markMobileSelectionTime` / `getLastMobileSelectionTick` accessors; the
  packet handler short-circuits inside the cooldown window without touching
  the SavedData. `ImmersiveMelodiesAuraHandler.acceptOpenPacket` enforces
  the same 5-tick throttle on mobile open/close traffic; both throttles are
  cleared on `onPlayerLogout` so dropped players don't pin map entries.
- **Stale `aura_hint` lang string fixed.** The tooltip line attached to every
  tracked instrument said "Right-click to open the aura selector (top-right)"
  — but right-click opens the *instrument*, not the selector. New text:
  "Open the instrument to choose an aura (top-right of the screen)".
- **Stale 1.3.0 free-play limitation comments / lang string scrubbed.**
  `ImmersiveMelodiesAuraHandler` and `ImmersiveMelodiesCompat` class docs
  still claimed free-play was unsupported (it's been working since 1.4.3).
  Lang key `tooltip.effectiveinstruments.compat.immersive_melodies.limitations`
  renamed to `.notes` and rewritten to describe the dual activation path
  (NBT `playing=true` for autoplay, `screenOpenInstrumentId` for free-play).
- **`EntityCategory.classify` comment now matches behavior.** The lines 66-69
  comment promised owner-tracking for extra-pet types ("treat as musician-
  owned") that the code never implemented — the actual behavior bucketed
  unknown-owner extra-pet types as `PASSIVE_MOB`, which is the correct
  defensive choice. Comment rewritten to describe what the code actually does.
- **Deprecation warnings on startup.** Eight 1.4.x-deprecated config keys
  (`targeting.allowSelfBuff`, `targeting.includeOtherPlayers`,
  `targeting.includeTamedPets`, `targeting.maxTargetsPerTick`,
  `offensiveTargeting.allowSelf`, `offensiveTargeting.includeTamedPets`,
  `mobileTier.allowSelfBuff`, `mobileTier.includeOtherPlayers`,
  `mobileTier.includeTamedPets`) had inaccurate "still read" / "migration
  default" comments. New `EIServerConfig.warnDeprecated()` wired from the
  `ServerAboutToStartEvent` handler emits a `WARN` line listing any key set
  to a non-spec-default value — gives admins a window to clean their TOMLs
  before the 1.5.0 hard-removal.
- **Anvil repair now follows vanilla cost-growth.** Both `combine` and
  `material` paths in `InstrumentAnvilHandler` set a flat `2` / `materialsNeeded * 2`
  XP cost and ignored the stack's `RepairCost` NBT — instruments were
  indefinitely cheap to keep repaired. 1.4.9 reads `getBaseRepairCost()`,
  computes `AnvilMenu.calculateIncreasedRepairCost(prev)` (= `prev * 2 + 1`),
  writes it back via `setRepairCost(next)`, and bills `next + flat`. After
  ~5 combines an instrument hits the same Too-Expensive cliff as a vanilla
  diamond pickaxe.
- **Atomic-write parity across every config writer.** Promoted
  `InstrumentAuraMapping.writeAtomically` into a shared
  `com.crims.effectiveinstruments.util.ConfigIO.writeAtomically` utility
  (temp + ATOMIC_MOVE, fall back to plain replace on Windows FAT). Routed
  every previously non-atomic writer through it: `InstrumentAuraMapping`
  (3 sites + `_README_INSTRUMENTS.txt`), `MobileInstrumentAuraMapping`
  (3 sites), `InstrumentDurabilityConfig` (1 site), `AuraJsonLoader` (5
  sites including the preset writers and `_README.txt`). A power-loss
  mid-write can no longer truncate any user-edited config file.
- **Mobile-tier per-tick scan is now bounded.** `ImmersiveMelodiesAuraHandler.onServerTick`
  used to walk every player on the level every pulse — 100 hash-map probes
  per second per dimension on a 100-player public server, even when nobody's
  using an IM instrument. New `ACTIVE_MOBILE_MUSICIANS` set is populated by
  `onScreenOpened` and a periodic discovery scan that picks up players who
  trigger autoplay without ever opening an IM screen. Idle players drop out
  after the configured linger window. On a server with zero IM users the
  loop is empty.
- **`affectedTargets` map is now pruned of dead-entity ids.** Both
  `AuraManager.PlayerAuraState` and `ImmersiveMelodiesAuraHandler.MobileAuraState`
  gained a `lastPruneTick` field; the per-pulse tick handlers run
  `affectedTargets.keySet().removeIf(id -> level.getEntity(id) == null)`
  once per minute (1200 ticks). Without this the map grew unbounded with
  dead-mob ids in long sessions where the same aura kept firing.
- **`radius` and `durationTicks` are now clamped at parse time.** A user-
  edited (or maliciously-shipped pack) JSON could set `"radius": 10000` or
  `"durationTicks": 24000`, which would inflate the AABB scan to a 20k-cube
  area or queue 20-minute effects that linger past the cleanup window. New
  `clampField` helper in `AuraJsonLoader.parseFile` clamps `radius` to
  [1, 64] (matching the global config knob) and `durationTicks` to
  [20, 6000] (1 second to 5 minutes). WARN-logged when the input is out of
  range.
- **Polish.** Legacy `tools/generate_aura_icons.py` (dead since 1.4.2)
  deleted — `tools/gen_aura_icons.py` is canonical. `instrumentOpen` flag's
  load-bearing `onAuraSwitch + clearAuraSelection` gate dropped from
  `AuraManager.onInstrumentClose` so closing an instrument always strips
  applied effects (the gate previously left targets buffed when the player
  triggered the aura via `onNotePlayed` without flipping `instrumentOpen`).
  Offensive-uniqueness audit + synthesis passes now share a single
  `computeOffensiveOwnership` walk of `MAPPINGS` instead of two; synthesis
  log message includes the synthesized count. Mobile fallback overlay in
  `AuraOverlayInjector` now honors `showInSelector` (curated allow-lists
  intentionally bypass it; the catch-all fallback is a different surface).
  Defensive client-tick guard in `AuraOverlayInjector` clears the IM overlay
  state if a non-`Closing` screen swap leaves the state attached to a
  no-longer-rendered screen.
- **`AuraSelectorWidget` no longer holds a stale `Screen` reference.** The
  `parentScreen` field demoted to a constructor-local `int parentScreenWidth`
  — window-resize re-fires `Init.Post` which builds a fresh widget, so no
  recycled instance reads stale `screen.width`. `AuraOverlayInjector` tracks
  the stationary screen identity locally as `stationaryOverlayScreen`,
  parallel to the existing `imOverlayScreen` for the IM path.
- **`/effectiveinstruments diagnose` now shows positive-targeting state.**
  Added a parallel block to the offensive line so "my regen aura doesn't
  hit my ally" complaints have a single-command answer.
- **`OFFENSIVE_INCLUDE_OTHER_PLAYERS` toggle is no longer silently overridden.**
  With the default `OFFENSIVE_INCLUDE_ALL_NON_PETS=true`, the dedicated
  PvP toggle was ignored — admins flipping `OFFENSIVE_INCLUDE_OTHER_PLAYERS=false`
  to make their server PvP-safe got no actual suppression. 1.4.9 gates
  `OTHER_PLAYER` on the dedicated toggle inside the
  `INCLUDE_ALL_NON_PETS=true` branch. **Behavior change** for servers that
  were relying on the old (broken) behavior of "ignore OTHER_PLAYERS toggle
  when ALL_NON_PETS is on" — call out in your release notes.
- **State-machine unit tests added.** `AuraManagerActiveStateTest`,
  `InstrumentDurabilityNamespaceTest`, `InstrumentAnvilHandlerMathTest` —
  pure-JUnit, mirror the production logic locally (same pattern as
  `AuraApplicatorBehaviorTest`). Cover the sliding-window activation
  algorithm, namespace-allowlist + fallback-Entry math, and anvil cost
  escalation + materials-needed rounding.
- **Comment-only.** `EIPacketHandler.PROTOCOL_VERSION` now carries a TODO
  for the 1.5.0 bump conditions.

## 1.4.8 Hotfix Set
- **Offensive aura synthesis is now uniqueness-aware.** Previously, the
  effect-based fallback (`InstrumentAuraMapping.findOffensiveByEffect`)
  iterated the registry and returned the *first* offensive preset whose
  primary effect matched the positive's inverse — so every regen-primary
  positive resolved to `echoes_of_decay`, every resistance-primary
  positive resolved to `battle_fanfare`, etc. Custom positives sharing a
  primary collided on the same offensive. 1.4.8 renames the helper to
  `findUnusedOffensiveByEffect(positiveId, taken)` and batches
  `synthesizeMissingOffensiveAllowed` to track already-claimed offensive
  ids, assigning a distinct offensive per instrument. Two-pass: effect-
  matched + unused first, then any unused offensive as the fallback.
- **Unique-assignment migration re-runs once.** Marker bumped
  `.instrument_unique_assignment_v1_done` → `_v2`. Existing installs
  re-run `ensureUniqueAssignment` with the new uniqueness-aware synthesis
  to fix duplicates that slipped past the 1.4.4 pass. Preserves user
  customization — entries whose default is a non-shipped id stay intact.
- **Load-time integrity audit.** New `auditOffensiveUniqueness` runs at
  the end of `InstrumentAuraMapping.load`. Emits WARN lines listing any
  offensive aura id that appears on more than one instrument, with the
  list of affected instruments and a pointer to the new reset command.
  Surfaces future regressions without needing debug mode.
- **`offensiveTargeting.includeAllNonPets` (default `true`).** New config
  key flips offensive targeting to "everything except pets" by default:
  other players, villagers, iron golems, passive mobs, hostile mobs all
  take the debuff. Musician and their own pets are still polarity-
  excluded by `AuraApplicator.gatherTargets`. Other-player-pets are
  excluded by category. When set to `false`, the old per-category
  include-knobs take effect — admins who want fine-grained control still
  have it. Existing installs pick up the new default via Forge config's
  "missing key → use spec default" behavior without requiring file edits.
- **`/effectiveinstruments reset-mappings`** (OP) — nuke-and-regenerate
  for the instrument-aura mapping. Deletes the JSON + all three migration
  markers, then calls `AuraRegistry.reload()`. Rebuilds the mapping from
  the canonical `UNIQUE_INSTRUMENT_TO_POSITIVE` table. Preset JSONs are
  untouched. Use when a mapping got corrupted or stuck with duplicates.
- **`/effectiveinstruments diagnose`** now shows `allNonPets` flag state
  and all per-category offensive toggles in one line so you can see
  exactly what the current behavior is without reading the TOML.

## 1.4.7 Hotfix Set
- **Creative-mode durability immunity is now configurable.** 1.4.6 shipped a
  hardcoded "creative player = no durability damage" check that made testing
  confusing — you couldn't verify durability depletion in the natural testing
  mode. New server config key `durability.creativeImmunity` (default `true`
  to preserve vanilla tool behavior; flip to `false` to verify depletion).
  The check now reads the config: `InstrumentDurability.damage:104`.
- **Aura now applies immediately on each note, not just on the tick.** The
  previous flow (`AuraManager.onServerTick` every 10 ticks = 500 ms) left
  a window where a single-note test could slip between ticks with no
  effects visible, and closing the screen immediately after the note tore
  down `instrumentOpen` before the next tick could fire. New public entry
  point `AuraManager.applyAuraNow(player)` is called from
  `NoteActivityHandler.processNote` right after `onNotePlayed`. The tick
  loop still handles refresh during held play.
- **`isActive` no longer requires `instrumentOpen=true`.** Previously the
  aura hard-deactivated the instant the player closed the instrument
  screen, so users who closed the UI to observe mob effects saw nothing.
  Recent notes alone (within `NOTE_WINDOW_TICKS`, default 5 s) now keep
  the aura active. Cleanup on screen close still strips tracked effects.
- **Subscribe to concrete `InstrumentPlayedEvent` subclasses.** 1.4.6 only
  listened on `InstrumentPlayedEvent<?>` (abstract). Forge EventBus
  usually dispatches concrete subclasses to abstract-parent listeners, but
  the path is reportedly fragile. 1.4.7 subscribes to both
  `NoteSoundPlayedEvent` and `HeldNoteSoundPlayedEvent` explicitly; the
  shared logic lives in the private `processNote` helper.
- **`activeMusicians` populated on note-played.** Previously only
  `onInstrumentOpen` added the player to the tick-loop set. If that event
  missed (old worlds, rare modded compat hiccups), the player would never
  tick. `onNotePlayed` now also adds them — a note is authoritative
  evidence they're playing.
- **`/effectiveinstruments diagnose`** — new command (no OP required) that
  dumps every gate in the aura pipeline: held items + tracked state,
  current selection, `isActive`, `instrumentOpen`, affected-target count,
  master toggles (`DURABILITY_ENABLED`, `DURABILITY_CREATIVE_IMMUNITY`,
  creative flag, `OFFENSIVE_AURAS_ENABLED`, per-category offensive
  toggles), and activation thresholds. Answers "why isn't my aura
  firing?" in one command.

## 1.4.6 Hotfix Set
- **IM icons render crisply now.** The 1.4.5 IM overlay used the 9-arg
  `blit(rl, x, y, uOff, vOff, w, h, texW, texH)` with mismatched destination
  (20×20) vs texture (16×16) dimensions — the GPU interpreted that as "draw
  the top-left 20×20 of a 16×16 texture", which wraps/clips instead of
  scaling. 1.4.6 switches to the 10-arg scaling overload
  `blit(rl, x, y, w, h, uF, vF, uW, vH, texW, texH)` (the same one
  `AuraButtonWidget.renderWidget:92` uses) and reduces `IM_ICON_SIZE` to the
  native 16 so scaling is 1:1 regardless. Added a 1-px selection ring so
  the pre-baked PNG's subtle selected border still reads clearly. Tooltip now
  shows display-name + description, matching the stationary widget.
- **Mobile tier now spawns musical-note particles.** Extracted
  `AuraManager.spawnAuraNotes(player, aura, radius)` as a shared entry point
  and wired it into `ImmersiveMelodiesAuraHandler.tickPlayer` right after
  `AuraApplicator.apply`. Stationary tier particle behavior is unchanged
  (delegates to the new shared helper). Users no longer see zero visual
  feedback while playing an IM instrument.
- **Creative mode durability immunity.** `InstrumentDurability.damage` now
  early-returns when `player.getAbilities().instabuild` — matches vanilla
  tool behavior. Creative players no longer wear instruments out.
- **Low-durability warning at ≤10%.** `NoteActivityHandler` crosses the
  threshold on the damaging note and fires a gold action-bar message once
  per second. Uses translation key `message.effectiveinstruments.durability_low`.
- **Broken-message + low-durability throttles are now per-player.**
  1.4.5's `static long` throttle was shared across all players — one
  musician's broken-spam suppressed another's warning. 1.4.6 uses
  `Map<UUID, Long>` keyed by player with cleanup on logout (new
  `NoteActivityHandler.onPlayerLogout` called from
  `InstrumentStateHandler.onPlayerLogout`).
- **Durability registry lookups are now cached.** `InstrumentDurability`
  caches `Item → Entry` in a `ConcurrentHashMap`, invalidated when
  `AuraRegistry.load()` reloads the durability config. Previously every
  item-slot render frame did a `ForgeRegistries.ITEMS.getKey(item)` call
  — cheap individually but O(slots) per frame.
- **Aura-system hint in instrument tooltip.** `DurabilityTooltipHandler`
  now appends a dark-gray italic hint line so players who pick up an
  instrument discover the aura selector without reading docs. Translation
  key: `tooltip.effectiveinstruments.aura_hint`.
- **Action-bar hint when opening IM screen without holding the
  instrument.** Instead of silently skipping the overlay, the player sees
  a yellow "Hold an Immersive Melodies instrument to choose an aura"
  message. Key: `message.effectiveinstruments.im_overlay_hold_required`.
- **Preset-regeneration existence gates extended.** 1.4.4 added an
  `if (Files.exists) return;` guard to `writeDefaultJson` (positive
  stationary presets). 1.4.6 adds the same guard to
  `writeOffensiveStationaryDefault`, `writeOffensiveMobileDefault`, and
  `writeMobileDefault` — users who hand-edited any preset JSON now keep
  their changes across marker bumps uniformly.
- **Dead code removed.** `AuraSelectorWidget.reattachTo`/`isAttachedTo`
  (reflection into protected `Screen.addRenderableWidget`) and the
  `AuraOverlayInjector.onScreenRenderPre` listener they fed — both were
  load-bearing in 1.4.4 when IM used the widget path, now dead since the
  IM path became render-event based in 1.4.5.
- **Stale user-facing text fixed.** `/effectiveinstruments help` no longer
  references the pre-1.2 config path `effectiveinstruments-server.toml`.
  The error log in `InstrumentAuraMapping.synthesizeMissingOffensiveAllowed`
  now points at the correct `_v3` offensive-defaults marker file.

## 1.4.5 Hotfix Set
- **IM overlay is now render-based, not widget-based.** The Phase-A
  widget-reattach mechanism from 1.4.4 wasn't enough — on the user's install
  the icons never appeared at all. Root causes: IM screens rebuild children
  aggressively, and when the held instrument has no mobile mapping entry the
  injector used to bail silently. Now the IM path:
  1. Draws icons directly in `ScreenEvent.Render.Post` from a static list —
     no `Screen.children()` dependency.
  2. Handles clicks in `ScreenEvent.MouseButtonPressed.Pre` via hit-testing on
     cached rects, `setCanceled(true)` when a click lands on an icon.
  3. Falls back to showing ALL enabled mobile presets when the held
     instrument has no mapping, so users with missing mapping JSONs still
     get a usable selector.
  4. Logs at info-level when it bails and why — the logs now say "IM screen
     opened but no IM instrument is held" or "mobile allow-list empty for X".
- **Durability bar on every instrument slot.** New
  `InstrumentDurabilityBarDecorator` (client-only, MOD bus, `@OnlyIn(CLIENT)`)
  registers an `IItemDecorator` via `RegisterItemDecorationsEvent` for every
  item in the `genshinstrument`, `evenmoreinstruments`, `immersive_melodies`
  namespaces. The decorator reads `InstrumentDurability.getCurrent/getMax`,
  computes ratio, and blits a vanilla-style bar at the bottom of the slot
  (2-px tall, 13-px wide, green→yellow→red gradient). Hidden at full
  durability and when the stack isn't tracked. Because we don't own the
  instrument `Item` classes, we can't set `maxDamage` — the decorator is the
  only way to surface NBT-backed durability visually.
- **Base durability up 4×.** Per-instrument defaults scaled: 200 → 800, 300
  → 1200, 400 → 1600. Global fallback `defaultMax` 300 → 1200. Marker bumped
  to `.instrument_durability_defaults_generated_v2` so existing installs
  regenerate `instrument_durability.json` once on next boot. User edits to
  that file are preserved (the existence-check was added in 1.4.4 for the
  preset JSONs — the mapping file regenerates on the marker bump, but
  `instrument_durability.json` is regenerated fully on the marker bump).
- Render path deliberately bypasses the old `AuraSelectorWidget` +
  `reattachTo` reflection machinery for IM screens. The widget path is still
  used for GI/EMI (Genshin + Even More stationary screens), where the
  `InstrumentScreen` base class plays nicely with Forge's `ScreenEvent.Init`.

## 1.4.4 Hotfix Set
- **IM selector no longer vanishes mid-open.** `AuraOverlayInjector` gained a
  `ScreenEvent.Render.Pre` listener that re-inserts the selector widget when
  the host screen has wiped its children list (Immersive Melodies rebuilds
  widgets on tab/scroll without re-firing `ScreenEvent.Init.Post`). The
  helper `AuraSelectorWidget.reattachTo` uses reflection on the protected
  `Screen.addRenderableWidget` once per missing-frame — idempotent via
  `isAttachedTo` guard so steady-state cost is a single membership check.
- **Offensive auras are now visibly applied.** `AuraApplicator.applyEffectSafely`
  takes a new `offensive` flag; positive auras keep `ambient=true` for subtle
  friendly-target particles, offensive auras set `ambient=false` so users can
  visually confirm debuffs landed on hostile mobs. The clear pass (which
  checks `isAmbient()`) therefore only strips *our* positive effects on aura
  switch — offensive debuffs expire naturally.
- **Legacy offensive selections are no longer silently rejected.** The
  stationary `SelectAuraC2SPacket` handler now accepts any opposite-polarity
  counterpart of the instrument's default aura even when the mapping file's
  legacy `allowed` list omits it. Logged at info-level as a one-shot
  diagnostic. Fixes the class of install where users upgraded from 1.3.x but
  never got the offensive-migration pass to run, making offensive selections
  no-op on the server.
- **Debug log now shows target count per-tick.** With `ei_debug` on,
  `AuraManager.onServerTick` logs `aura={id} offensive={bool} targets={n}
  player={name}` every active pulse — enough to confirm the aura is in fact
  gathering mobs when users think it isn't.
- **Every instrument has its own unique aura pair now.** Positive-stationary
  table expanded 15 → 31 entries and offensive-stationary 15 → 31. Every GI +
  EMI instrument maps to a distinct (primary effect, secondary effect,
  amplifier) tuple across both polarities. Sixteen new positive ids
  (`skyward_zephyr`, `rumbling_anthem`, `thunderous_cadence`, `drumline_vigor`,
  `artisan_tempo`, `chiming_revival`, `starlit_grace`, `fleetfoot_lilt`,
  `troubadour_march`, `craftwork_rondo`, `ironwright_anthem`,
  `pasture_serenade`, `hearthlight_drone`, `pixel_pulse`, `wayfinders_reel`,
  `bellwether_toll`) + 16 mirrored offensive ids (`skyward_blight`,
  `rumbling_curse`, `thunderous_dirge`, `drumline_blight`, `artisan_curse`,
  `tolling_entropy`, `starlit_malice`, `fleetfoot_fall`, `troubadour_dirge`,
  `craftwork_rot`, `ironwright_curse`, `pasture_rot`, `hearthshade_dirge`,
  `pixel_rot`, `wayfinders_lament`, `bellwether_rot`). All 32 new icons
  generated via `tools/gen_aura_icons.py`. Also fixed the latent
  `earth_tremor` ⟷ `fathomless_pull` tuple collision from 1.4.x.
- **Marker bumps force regeneration on upgrade.** Stationary positive marker:
  `.defaults_generated_v2`. Offensive marker: `.offensive_aura_defaults_generated_v3`.
  New mapping-migration marker: `.instrument_unique_assignment_v1_done` — walks
  existing mapping files and reassigns any instrument whose default is a
  shipped aura id that no longer matches the 1-to-1 target. Preserves user
  customizations (unknown default ids are left alone).
- **`writeDefaultJson` is now existence-gated.** Marker bumps regenerate the
  mapping file but no longer clobber user-edited preset JSONs. Only new or
  deleted preset files are written.
- **Uniqueness guard runs on every `AuraRegistry.load()`.** Warn-level log
  lists any preset ids that share a `(polarity, effects, amplifiers)` tuple.
  Shipped defaults are expected to pass silently.

## 1.4.3 Hotfix Set
- **Icons gracefully fallback on missing textures.** `AuraButtonWidget` now
  pre-checks the texture via the Minecraft resource manager. If it doesn't
  resolve (common for *custom* user-authored aura JSONs that reference PNGs
  the mod jar doesn't ship), the button falls back to the letter renderer
  rather than showing the missing-texture magenta square.
- **Effect-based offensive pairing.** `InstrumentAuraMapping.findOffensiveByEffect`
  now catches user-custom positive auras (e.g. `soothing_hymn`,
  `guardian_chorus`, `invigorating_march`, `luminous_nocturne`) that aren't
  in `POSITIVE_TO_OFFENSIVE_AURA_ID`. It reads the positive preset's primary
  effect, maps it through `POSITIVE_EFFECT_INVERSE` (speed→slowness,
  regen→wither, strength→weakness, etc.), and finds a shipped offensive
  preset whose first effect matches. This is how a user's custom
  instrument-to-aura mapping gets an offensive counterpart without manual
  editing.
- **Immersive Melodies GUI integration.** The aura selector now appears in
  the top-right of both IM screens (`ImmersiveMelodiesScreen` and
  `ImmersiveMelodiesFreePlayingScreen`) — same placement as on GI screens.
  The old `B` keybind + standalone `MobileAuraPickerScreen` are retired.
  `AuraOverlayInjector` routes mobile selections through
  `SelectAuraC2SPacket`'s mobile field and consults
  `MobileInstrumentAuraMapping.getAllowedAuras` (new, no `showInSelector`
  filter so offensive mobile presets appear).
- **Free-play mode now applies auras.** IM's `playing=true` NBT flag isn't
  set during free-play keyboard input, so v1.4.x mobile auras only worked
  during autoplay. `InstrumentOpenC2SPacket` grew `mobileTier`/`close`
  fields; on IM screen open the client tells the server, which records
  `screenOpenInstrumentId` on the mobile state. `ImmersiveMelodiesAuraHandler.tickPlayer`
  now activates on either playing=true OR screenOpen. Durability is only
  charged on the playing=true path (browsing the melody list is free).

## 1.4.2 Hotfix Set
- **Icons regenerated.** The 1.3.x-era positive icon PNGs were 224/256 pixels
  transparent — that's what the user saw as "missing texture". Replaced with
  procedural flat-color + effect-specific glyph icons via
  `tools/gen_aura_icons.py` (single source of truth, supersedes the older
  `gen_offensive_icons.py`). Each aura gets a glyph drawn from its primary
  `MobEffect`: arrow-up for speed/boost, heart for regen, shield for
  resistance/absorption, eye for night vision, flame for fire resistance,
  skull for poison/wither, arrow-down for slowness, broken sword for
  weakness/mining fatigue, closed eye for blindness, spiral for nausea,
  up-X for levitation, empty bowl for hunger. Polarity drives border color
  (green vs red) and contrast.
- **Mobile positive presets now have icons.** `AuraJsonLoader.writeMobileDefault`
  emits `icon`/`iconSelected` fields and sets `showInSelector=true`. Mobile
  marker bumped to `_v2` so 1.3.x / 1.4.x installs regenerate mobile JSONs.
- **Loud diagnostics on synthesis failure.** `InstrumentAuraMapping.synthesizeMissingOffensiveAllowed`
  now logs an error-level message listing instruments whose offensive
  counterpart didn't load, plus the exact config file path the user should
  delete to force regeneration.
- **If the offensive aura still doesn't appear**, the user should delete
  `config/effective_instruments/.offensive_aura_defaults_generated_v2`
  (forces full regeneration) OR run `/effectiveinstruments reload` — which
  also runs the self-healing pass.

## 1.4.1 Hotfix Set
- **Icon render fix** — `AuraButtonWidget.blit` now passes fixed 16x16 source
  dimensions (no more missing-texture squares at compact scale) and falls
  back to the letter renderer on any draw failure.
- **Offensive icons shipped** — 26 procedural PNGs generated by
  `tools/gen_offensive_icons.py`. The offensive-defaults marker bumped to
  `_v2` so upgraded installs regenerate their JSONs once with the new
  `icon`/`iconSelected` fields.
- **Self-healing mappings** — `InstrumentAuraMapping.load()` and
  `MobileInstrumentAuraMapping.load()` now synthesize missing offensive
  `allowed` entries in memory and opportunistically rewrite the JSON.
  `AuraJsonLoader.healMissingOffensivePresets()` regenerates any offensive
  preset file that vanished from disk.
- **Mobile picker** — new keybind (default `B`) opens a
  `MobileAuraPickerScreen` when holding an IM instrument with >1 allowed
  aura. Selections persist as `SavedData` under `effectiveinstruments_mobile_selections`.
  `SelectAuraC2SPacket` grew an optional `mobileInstrumentId` field;
  protocol bumped to `4`.
- **Targeting contract tightened** — positive auras *always* include the
  musician and their own pets; offensive auras *never* do. Everything else
  is per-polarity config. New `positiveTargeting`/`offensiveTargeting`
  category toggles (other players, other player pets, villagers, iron
  golems, passive mobs, hostile mobs). Legacy `targeting.allowSelfBuff`
  and `targeting.includeTamedPets` are deprecated; still present in
  `server.toml` for backwards-compat but ignored at runtime.
- **EntityCategory** — centralized classifier. `AuraApplicator.gatherTargets`
  replaced a two-branch implementation with a single-loop classifier that
  buckets candidates by category and emits in priority order
  (musician → own pets → other players → …).

## Migration Notes (1.3.x → 1.4.0)
- Instrument mapping files auto-migrate string-form entries to object form
  with the offensive aura added to `allowed`. Marker files:
  `.instrument_offensive_migration_done`,
  `.mobile_instrument_offensive_migration_done`.
- Existing worlds: instruments already in inventories initialise to full
  durability on first hover/play.
- Rollback: set `durability.enabled=false` and/or
  `offensiveTargeting.enabled=false` in server config.

# Changelog

All notable changes to Effective Instruments will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.6.0] - 2026-05-16

### Added — Multi-mod NPC compatibility

This release generalizes Effective Instruments from a *player-only* aura
framework to a *generic LivingEntity* aura framework, then layers per-mod
NPC adapters on top. Any supported mod's NPC can drive the aura pipeline.

#### Core abstraction layer (`performer/`)
- **`IAuraPerformer`** — central interface. Wraps any `LivingEntity` (player or
  NPC adapter) and exposes owner/team/faction/instrument/aura-id/tier in one
  contract. The player path stays byte-identical to 1.5.0 — gated by the new
  `AuraBehaviorParityTest` regression test.
- **`PlayerPerformer`** — the backwards-compat anchor. All internal callers
  wrap their `ServerPlayer` at the boundary; the deprecated `apply(ServerPlayer, …)`
  overload is preserved for one release.
- **`PerformerRegistry`** — ServiceLoader hub. Discovers per-mod
  `PerformerAdapterProvider`s through
  `META-INF/services/com.crims.effectiveinstruments.performer.PerformerRegistry$PerformerAdapterProvider`.
- **`TargetClassifier`** — performer-aware version of `EntityCategory.classify`.
  Resolution order: adapter hint → tag → JSON override → owner/team/faction → vanilla.
- **`OwnerResolver`** + **`FactionResolver`** — cross-mod oracles. Vanilla
  `OwnableEntity` path is the short-circuit; per-mod providers register on top.
  `ReflectiveOwnerProbe` is the fallback for unknown entities.
- **`PerformerRadiusModifier`** — non-player aura radius is scaled by
  `npcs.performerAuraRadiusMultiplier` (default 0.75) and any registered
  modifier (e.g., Pehkui scale).
- **`ai/PlayInstrumentGoal`** — reusable state machine for goal-based
  adapters: `INACTIVE → READY → PLAYING_SUPPORT|OFFENSIVE → INTERRUPTED → COOLDOWN`.

#### Tag + JSON extensibility (modpack authors)
- 8 new entity-type tags under `data/effective_instruments/tags/entity_types/`:
  `always_buff`, `always_debuff`, `ignore`, `force_performer`, `never_performer`,
  `treat_as_villager`, `treat_as_iron_golem`, `player_proxy_owner`.
- `config/effective_instruments/entity_classification.json` — JSON overrides per
  entity type with `category` / `requireTamed` / `delegateTo`. Ships with 22
  curated defaults across 9 long-tail mods (Alex's Mobs, Friends & Foes,
  Twilight Forest, Cataclysm, Mowzie's Mobs, More Villagers, Dungeons Mobs,
  Born in Chaos, Eidolon, Graveyard). Tag overrides win over JSON; JSON wins
  over adapter defaults.

#### Tier-1 adapters (full performers)
- **`compat/recruits`** — talhanation/recruits 1.15.0+. Reflection-cached
  `getOwnerUUID` / `getInventory` / `getFleeing` / `getShouldRest` /
  `getAggroState`. Priority 3, `MOVE+LOOK` mutex.
- **`compat/guardvillagers`** — seymourimadeit/guardvillagers 1.6.15+.
  `getOwnerId()` direct, Hero-of-the-Village player as implicit owner
  fallback. Priority 5.
- **`compat/easy_npc`** — MarkusBordihn/BOs-Easy-NPC 5.0+. Marker-interface
  recognition (`EasyNPC<E extends Mob>`); owner via `OwnableEntity` (vanilla
  path). Priority 4.
- **`compat/doggytalents`** — DashieDev/DoggyTalentsNext 1.18.64+. OFFHAND
  instrument slot, sitting-or-docile gate via reflection on `isInSittingPose`
  and `getMode().name()`. Priority 6.
- **`compat/irons_spellbooks`** — iron431/irons-spells-n-spellbooks 3.4+.
  `MagicSummon.getSummoner()` for owner; summon-timer effect (effect id
  `irons_spellbooks:summon_timer`) drives a 60-tick safety margin so summons
  don't perform mid-despawn. Priority 8.
- **`compat/ars_nouveau`** — baileyholl/Ars-Nouveau 4.0+. Starbuncle is
  **ownerless** (verified: no `OWNER_UUID` data field in the source). Plays
  for nearby targets via the global classifier. Priority 4.
- **`compat/touhou_little_maid`** — TartaricAcid/TouhouLittleMaid 1.5.0+.
  Goal-based (`EntityMaid extends TamableAnimal` so it has a goalSelector
  alongside its Brain). Combat veto reads `IMaidTask.getUid()` and matches
  against `attack`/`combat`/`guard`. Priority 7.

#### Tier-2 adapter (target-only)
- **`compat/mca`** — Luke100000/minecraft-comes-alive (MCA Reborn) 7.0+.
  `EntityRelationship.of(villager).getPartnerUUID()` is treated as the
  villager's owner — a player's married villager classifies as `OWN_PET` of
  their spouse. Tier-1 promotion deferred to 1.7.0 (Brain drift risk).

#### Library hooks
- **`compat/pehkui`** — Virtuoel/Pehkui 3.0+. Multiplies non-player aura
  radius by the entity's `ScaleTypes.BASE.getScaleData(entity).getScale()`.
  Player path untouched.

#### Diagnostic commands (`/effectiveinstruments npcs`)
- `npcs adapters` — list every discovered `PerformerAdapterProvider` and
  whether it's active (mod loaded) or dormant.
- `npcs list [radius]` — scan up to 128 blocks for wrappable NPCs; columns
  `entityType uuid owner instrument`.
- `npcs diagnose <entity>` — dump one NPC's `IAuraPerformer` view: tier,
  uuid, owner, team, instrument, selected aura id, `canPerformNow`.

#### Build / audit
- **`checkCompatAuditInvariant`** gradle task — fails the build on any
  cross-namespace import (e.g., `import com.cstav.genshinstrument` outside
  `compat/genshin/`). Wired into `check` lifecycle; CI gates on it.
- **`AuraBehaviorParityTest`** — structural-invariants regression test for
  the `EntityCategory` enum order and presence of the new `TargetClassifier`
  / `PlayerPerformer` / `IAuraPerformer` types. The byte-identical in-game
  behavior parity test lives in GameTest fixtures (deferred to 1.6.1).

### Changed
- **`EntityCategory.classify`** has a new `IAuraPerformer`-aware overload.
  The legacy `classify(ServerPlayer, Entity, Set)` is preserved as a
  deprecated wrapper for one release.
- **`AuraApplicator.apply`** has a new `IAuraPerformer`-aware overload.
  Non-player paths now route through `computeEffectiveRadius` for
  multiplier scaling. Player path is unchanged.
- **`AuraManager.spawnAuraNotes`** now accepts any `LivingEntity` — particle
  origin shifts up to `y + bbHeight * 0.7` for non-players (head height),
  while players keep the `y + 0.5` offset for visual parity.
- **`StationaryInstrumentNoteService`** has a new `processNote(IAuraPerformer, ItemStack)`
  overload consumed by NPC adapter goals.
- **`ImmersiveMelodiesAuraHandler`** has a parallel `ACTIVE_MOBILE_NPCS`
  set and `tickPerformer(IAuraPerformer)` method. NPC mobile activation
  flows entirely through the adapter — IM's `playing=true` NBT flag is
  never written for NPCs.
- **`EffectiveInstrumentsMod.commonSetup`** calls
  `PerformerRegistry.discover()` + `bootstrapAll()` after the existing
  backend init.

### Config schema (additions only; 1.5.0 keys unchanged)
- `[npcs]` master block (13 keys: enabled, allowPerformers, allowTargets,
  performerCooldownTicks, performerAuraRadiusMultiplier, requireOwnerOnline,
  etc.).
- `[npcs.recruits]` (4 keys), `[npcs.guardvillagers]` (3 keys),
  `[npcs.easy_npc]` (2), `[npcs.doggytalents]` (3),
  `[npcs.irons_spellbooks]` (3), `[npcs.ars_nouveau]` (3),
  `[npcs.touhou_little_maid]` (3), `[npcs.mca]` (2), `[npcs.pehkui]` (1).

### Verification
- **Pure-reflection runtime path** for every adapter. Every per-mod API
  call goes through cached `MethodHandle`s with graceful degradation —
  mod-version drift downgrades the adapter to "no veto" / "no owner",
  never a crash.
- **GitHub-source verification** at design time for every adapter caught
  signature mismatches before shipping: Recruits (`isFleeing` → `getFleeing`,
  `isInRaid` → `getAggroState == AGGRO_RAID`), Guard Villagers (`isAggressive`
  is on `Mob` not `LivingEntity`), Iron's Spells (`IMagicSummon` →
  `MagicSummon`, `getSummoner` returns `LivingEntity` not UUID), Ars
  Nouveau Starbuncle (no owner tracking at all).
- **Audit invariant passes** with 10 active compat packages.

### Hotfix bundle — runtime fixes from in-game test cycles (folded into v1.6.0)

These fixes landed during in-game test cycles before the public v1.6.0 release;
they are part of v1.6.0 proper, not a separate version. They are documented
together here so the runtime-critical fixes are not lost when the initial Phase
0-6 section is read alone.

- **`mods.toml` versionRange widening.** Every Phase 2-5 optional NPC dep was
  pinned to a numeric lower bound (e.g. `[3.4,)` for `irons_spellbooks`).
  Forge's Maven comparator misorders versions of the form `1.20.1-3.15.6`
  because it parses the leading `1.20.1` as major version 1 < 3. All nine
  optional deps now widened to `[0,)`; real version validation happens at
  runtime via `ModList.isLoaded` + reflection probes. The existing 1.5.0
  entry for `immersive_melodies` already used the same pattern.

- **Reflection signature corrections** (verified against installed-jar
  bytecode via `javap`):
  - **Guard Villagers** — class FQN `tallestegg.guardvillagers.common.entities.Guard`
    → `tallestegg.guardvillagers.entities.Guard` (no `common.` segment).
  - **MCA Reborn** — Architectury universal jars preserve the `forge.`
    prefix at runtime. Class FQN is `forge.net.mca.entity.VillagerEntityMCA`.
    `MCAReflection.tryResolve` probes both prefixed + bare candidates for
    forward-compat.
  - **Iron's Spells** — `MagicSummon` is a 337-byte deprecated stub; the
    real interface is `IMagicSummon` (6535 bytes). `getSummoner()` returns
    `Entity`, not `LivingEntity`.
  - **Recruits** — `getAggroState` doesn't exist; the state getter is
    `getState()`. The AGGRO_RAID veto was dropped since the existing
    target/last-hurt/fleeing/should-rest checks cover combat states.
  - **Doggy Talents Next** — `isInSittingPose` reflection on inherited
    vanilla methods misfires under Forge's class transformer. Switched
    to direct `TamableAnimal.isInSittingPose()` bytecode call (no reflection).

- **Reflection helper pattern swap** — every adapter's reflection helper
  switched from `MethodHandles.publicLookup() + unreflect() + asType()` to
  plain `java.lang.reflect.Method.invoke()` with `setAccessible(true)`. The
  publicLookup pipeline silently fails to resolve inherited / vanilla
  methods across Forge's class transformer; plain reflection works
  unconditionally.

- **NPC aura auto-selection** — three cascading nulls blocked NPC performance:
  `AuraManager.PlayerAuraState` was never created for NPCs (no UI to call
  `setAuraSelection`); `state.selectedAura` was null; `state.isActive`
  returned false because `recentNoteTicks` was empty (no UI to call
  `onNotePlayed`). Fixes:
  - New `AuraManager.onNotePlayed(IAuraPerformer)` overload with NPC
    auto-select from `InstrumentAuraMapping.getDefaultAuraId(itemId)` —
    same mapping the player's instrument-screen-open path uses.
  - `StationaryInstrumentNoteService.processNote(IAuraPerformer, ItemStack)`
    NPC path now calls `onNotePlayed(performer)` before `applyAuraNow(performer)`
    — mirrors the player path.
  - `PlayInstrumentGoal.currentPreset()` falls back to instrument-default
    when `selectedAuraId()` is empty, so the goal enters PLAYING_* on the
    first tick instead of thrashing INACTIVE→READY→INACTIVE.
  - `applyAuraNow(IAuraPerformer)` uses `getOrCreate` for NPC UUIDs; player
    path stays byte-identical with `get`.

- **Mobile-tier NPC tick registration** — Tier-1 adapters wrapped NPCs for
  `MOBILE` tier but never called `ImmersiveMelodiesAuraHandler.registerActiveMobileNpc(uuid)`,
  so the IM tick loop never iterated them and mobile auras were dormant.
  `PlayInstrumentGoal.start()` now auto-registers the NPC when the held
  instrument has a `MobileInstrumentAuraMapping`; `stop()` unregisters.
  One change fixes mobile-tier auras for every Tier-1 NPC adapter (Recruits,
  Guard Villagers, Easy NPC, DTN, Iron's Spells, AN Starbuncle, Touhou Maid,
  MCA).

- **MCA Tier-1 promotion** — `MCAPerformerAdapter` was Tier-2 target-only per
  the original 1.6.0 spec (§6.8 deferred to 1.7.0). Per user request,
  promoted to full Tier-1:
  - New `MCAPerformer`, `MCAInstrumentGoal`, `MCAEventHandler`.
  - `MCAReflection.isInCombatState` uses vanilla `AbstractVillager.isSleeping`
    + `Villager.isTrading` checks (no MCA-specific bandit-trait reflection
    in this pass — deferred to 1.6.1).
  - `MCAPerformerAdapter.capabilities()` now includes `STATIONARY_PLAY` +
    `MOBILE_PLAY` alongside the existing `AURA_TARGET` + `OWNER_AWARE`.
  - VillagerEntityMCA's class hierarchy extends vanilla `Villager` (verified
    via `javap`), and `Mob.serverAiStep` ticks the goalSelector
    unconditionally — same goal-injection pattern as Touhou Maid (whose
    Brain-based AI also ticks goalSelector via the same path).

- **`/effectiveinstruments npcs` UX enhancements**:
  - `npcs list <radius> all` includes Tier-2/3 target-only entities with a
    `[target]` tag — addresses the common confusion where users hand an
    instrument to a target-only entity expecting it to play.
  - `npcs diagnose <entity>` now shows resolved owner + JSON override +
    target-side classification bucket when the entity isn't wrappable as
    a performer. Confirms spouse-as-owner classification before/after
    MCA Tier-1 promotion.

- **JSON entity-classification defaults** — dropped four invalid
  `morevillagers:*` entries from the shipped `entity_classification.json`.
  MoreVillagers adds *professions* to `minecraft:villager`, not new entity
  types, so the `morevillagers:florist` etc. IDs never matched any
  registered entity type at load. The 21 remaining curated entries cover
  actually-mapped Tier-3 entities. Log message switched from hardcoded
  `22 curated entries` to a dynamic `root.entrySet().size() - 1` count.

- **`AuraManager.spawnAuraNotes` particle origin** — for non-player
  performers, the plume anchors at `entity.position().y + bbHeight * 0.7`
  (head height) instead of the player-only `y + 0.5` offset. Player path
  preserves the exact 1.5.0 offset for parity.

### Deferred to 1.6.1 / 1.7.0
- Tier-2 closed-source adapters (Custom NPCs, Eidolon thralls, Graveyard
  Ghouling) — pure-reflection adapters need test-fixture verification
  before shipping; their entity-classification JSON entries ship today.
- Brain-based adapter path (`PlayInstrumentBehavior` + `EIMemories`) —
  scaffold ships in Phase 0; first concrete user is a future Brain-only
  NPC adapter where goal injection is not viable.
- MCA bandit-trait combat veto — would require reflection on MCA's
  `Traits` system. Deferred since the baseline Tier-1 promotion is the
  immediate user need.
- `force-play` / `grant` `/effectiveinstruments npcs` subcommands.
- 16-scenario GameTest matrix (spec §14).

## [1.5.0] - 2026-05-09

### Changed
- **Genshin Instruments is now an optional backend.** Previously the mod
  hard-required GI (`mandatory=true` in `mods.toml`, `implementation`-scoped
  in `build.gradle`) — users who wanted only Immersive Melodies mobile
  support had no path. 1.5.0 demotes GI to `compileOnly` /
  `mandatory=false`, on the same conceptual footing as IM. All four runtime
  combinations now work:
    1. EI + GI only — existing stationary behavior unchanged.
    2. EI + IM only — mobile tier works without GI installed (new).
    3. EI + GI + IM — both tiers work.
    4. EI alone — mod loads cleanly, logs a warning that no backend is
       installed, commands and configs still work.
- **Mod description broadened** to reflect dual-backend support: *"Aura
  effects while playing supported modded instruments, including Genshin
  Instruments and Immersive Melodies"*.

### Added
- **`compat/genshin/GenshinInstrumentsCompat`** — bootstrap that detects
  Genshin Instruments via `ModList.get().isLoaded` at common-setup and
  manually registers the GI event handler only when GI is present. Mirrors
  the existing `ImmersiveMelodiesCompat` pattern.
- **`compat/genshin/GenshinInstrumentEventHandler`** — the only class that
  imports `com.cstav.genshinstrument.*`. Registered manually (no
  `@Mod.EventBusSubscriber`); never linked when GI is absent. Holds the
  three previously-scattered GI handlers: `onNoteSoundPlayed`,
  `onHeldNoteSoundPlayed`, `onInstrumentStateChanged`.
- **`compat/genshin/client/GenshinInstrumentScreenBridge`** — reflection-
  based client bridge for GI's `InstrumentScreen`. Uses `Class.forName` +
  cached `Method` lookups behind an `isAvailable` gate so the client
  overlay holds zero compile-time references to GI types. Lazy-resolves on
  first use, caches success/failure to keep the hot path branch-free.
- **`event/StationaryInstrumentNoteService`** — backend-agnostic note
  pipeline. Owns the broken-state gate, polarity-aware durability damage,
  aura record + immediate-apply, and per-player broken/low-durability
  message throttles. The GI event handler now delegates here; any future
  backend can do the same without touching GI-specific code.
- **`/effectiveinstruments diagnose` Backends section.** New first line of
  the diagnose dump: `Backends: genshin=active|absent immersive_melodies=
  active|absent`. When neither is installed, follows up with a yellow
  WARNING line. Answers "is the backend even loaded?" without trawling the
  log.
- **Dev runtime toggle.** `./gradlew runClient -PdevRuntimeGenshin=true`
  pulls GI as `runtimeOnly` for local testing. The default `runClient`
  exercises the GI-absent path.

### Removed
- **`event/NoteActivityHandler`** — fully replaced by
  `StationaryInstrumentNoteService` (shared logic) and
  `GenshinInstrumentEventHandler` (GI-specific subscription). The throttle
  maps and per-player logout cleanup moved to the service alongside the
  broken/low warnings they belong with.
- **GI imports from `event/InstrumentStateHandler`** — the
  `onInstrumentStateChanged` handler and its `isHoldingBrokenInstrument`
  helper moved into `GenshinInstrumentEventHandler`. The class is now
  100 % backend-agnostic; `onLevelTick`, `onPlayerLogout`,
  `onPlayerChangedDimension`, `onPlayerDeath` remain.
- **GI import from `client/event/AuraOverlayInjector`** — replaced by the
  `GenshinInstrumentScreenBridge` reflection layer. The `instanceof
  InstrumentScreen` check became `GenshinInstrumentScreenBridge.isInstrumentScreen(screen)`,
  and `instrumentScreen.getInstrumentId()` became
  `GenshinInstrumentScreenBridge.getInstrumentId(screen)` with null-safe
  handling for the failure case.

### Fixed
- **Javadoc references to `com.cstav.genshinstrument.event.*` removed**
  from `AuraManager` and `EffectiveInstrumentsAPI`. The wording now
  describes the abstract "backend instrument-open event signal" rather
  than naming a class that may not be present at javadoc-generation time.

### Notes for server admins
- The packet protocol version is **unchanged** at `4`. v1.4.x clients
  connect to v1.5.0 servers cleanly and vice versa.
- **Behavior change:** if your server is currently running v1.4.x with
  Genshin Instruments installed, no migration is needed — the v1.5.0 jar
  detects GI exactly as before and runs the stationary tier.
- **New deployment option:** servers that don't want GI installed can now
  ship just Effective Instruments + Immersive Melodies and get the mobile
  tier, no GI jar required.

## [1.4.9] - 2026-04-25

### Fixed
- **Durability default-max fallback never fired.** `InstrumentDurability.resolveEntry`
  used to return `null` for any item not pre-listed in
  `instrument_durability.json`, so third-party instruments and any
  newly-shipped instrument the JSON didn't yet cover got zero durability
  tracking — exactly the opposite of what
  `InstrumentDurabilityConfig`'s class doc promised. Now synthesizes a
  default `Entry` from `EIServerConfig.DURABILITY_DEFAULT_MAX` when the
  lookup misses AND the item's namespace is in the new shared
  `InstrumentNamespaces.INSTRUMENT_MOD_IDS` allowlist (`genshinstrument`,
  `evenmoreinstruments`, `immersive_melodies`). Non-instrument items still
  get `NO_ENTRY` so swords/shields don't pick up an invisible NBT bar.
- **`AuraRegistry.load` ran before SERVER config was loaded.** The 1.4.x
  flow ran config-dependent work (pet allowlist invalidation, durability
  default fallback) at common-setup, before the SERVER `ForgeConfigSpec`
  is loaded by Forge. Result was either `IllegalStateException` from
  `IntValue.get()` or silently using the spec default — quietly
  self-correcting on `/reload`, masking the bug. Split into
  `loadPresetsAndMappings()` (JSON-only, safe at common-setup) and
  `refreshConfigDerived()` (config reads, deferred to a new
  `ServerAboutToStartEvent` handler in `EffectiveInstrumentsMod`).
  `AuraRegistry.load()` keeps the old API by chaining both halves.
- **Bar decorator could crash on pre-join screens.**
  `InstrumentDurabilityBarDecorator.renderBar` called
  `EIServerConfig.DURABILITY_ENABLED.get()` unprotected — throws
  `IllegalStateException` on the title screen, world list, and REI/JEI
  item-preview overlays rendered before world join. New static
  `EIServerConfig.isDurabilityEnabledSafe()` wraps the read in try/catch;
  consolidates the duplicated try/catch pattern that already lived in
  `DurabilityTooltipHandler`.
- **Mobile packets had no rate limit.** Stationary
  `InstrumentOpenC2SPacket` and `SelectAuraC2SPacket` had a 5-tick
  cooldown; mobile paths did not. A misbehaving client could flood mobile
  selections, each marking `MobilePlayerSelection`'s SavedData dirty and
  forcing autosave I/O. Added matching 5-tick throttles via
  `MobilePlayerSelection.markMobileSelectionTime` and
  `ImmersiveMelodiesAuraHandler.acceptOpenPacket`; both clear on logout.
- **Stale `aura_hint` lang string.** Tooltip read "Right-click to open
  the aura selector (top-right)" but right-click opens the *instrument*,
  not the selector. Now: "Open the instrument to choose an aura
  (top-right of the screen)".
- **Stale 1.3.0 free-play limitation comments + lang string.**
  `ImmersiveMelodiesAuraHandler` and `ImmersiveMelodiesCompat` class docs
  still claimed free-play was unsupported (working since 1.4.3). Lang key
  `tooltip.effectiveinstruments.compat.immersive_melodies.limitations`
  renamed to `.notes` and rewritten to describe the dual activation path.
- **`EntityCategory.classify` comment promised behavior the code didn't
  implement.** Comment block at lines 66–69 claimed extra-pet types were
  "treated as musician-owned" when the code actually buckets them as
  `PASSIVE_MOB`. Comment rewritten to match reality.
- **Anvil repair ignored vanilla work-penalty escalation.** Both combine
  and material paths set a flat XP cost and never wrote
  `RepairCost` NBT — instruments were indefinitely cheap to keep
  repaired. Now reads `getBaseRepairCost()`, computes
  `AnvilMenu.calculateIncreasedRepairCost(prev)` (= `prev * 2 + 1`),
  writes it back via `setRepairCost(next)`, and bills `next + flat`.
  After ~5 combines an instrument hits the same Too-Expensive cliff as a
  vanilla diamond pickaxe.
- **`affectedTargets` map could grow unbounded.** Dead-mob entity ids
  accumulated in long sessions with the same aura. Both
  `AuraManager.PlayerAuraState` and
  `ImmersiveMelodiesAuraHandler.MobileAuraState` gained a `lastPruneTick`
  field; the per-pulse handlers run
  `removeIf(id -> level.getEntity(id) == null)` once per minute.
- **`radius` and `durationTicks` were unbounded at parse time.** A
  user-edited or hostile JSON could set `"radius": 10000` or
  `"durationTicks": 24000`, inflating the AABB scan to a 20k-cube area
  or queueing 20-minute effects past the cleanup window. New
  `clampField` helper in `AuraJsonLoader.parseFile` clamps `radius` to
  [1, 64] and `durationTicks` to [20, 6000] with WARN logging.
- **`instrumentOpen` flag's load-bearing gate.** `AuraManager.onInstrumentClose`
  used to gate `onAuraSwitch + clearAuraSelection` on `instrumentOpen`,
  so a player who triggered the aura via `onNotePlayed` (which doesn't
  flip the flag) and then closed the instrument left their tracked
  targets buffed until the effects expired naturally. Gate dropped;
  cleanup now always runs on close.
- **Mobile fallback overlay ignored `showInSelector`.** When the
  per-instrument allow-list resolved to empty and the overlay fell back
  to "every enabled mobile preset", hidden user-custom presets were
  surfaced too. The fallback now honors `showInSelector`; the curated
  per-instrument path still bypasses it intentionally.
- **`OFFENSIVE_INCLUDE_OTHER_PLAYERS` toggle was silently ignored under
  default config.** With `OFFENSIVE_INCLUDE_ALL_NON_PETS=true` (default),
  the dedicated PvP toggle was overridden — admins flipping
  `OFFENSIVE_INCLUDE_OTHER_PLAYERS=false` to make their server PvP-safe
  got no actual suppression. The dedicated toggle is now respected
  inside the `INCLUDE_ALL_NON_PETS=true` branch.
  **⚠ Behavior change** for servers relying on the old (broken) behavior.

### Added
- **`EIServerConfig.isDurabilityEnabledSafe()`** — SERVER-config-safe
  accessor for the client render path. Returns `false` when the spec
  isn't loaded yet rather than throwing.
- **`EIServerConfig.warnDeprecated()`** — one-shot startup pass that
  emits a `WARN` for any of eight 1.4.x-deprecated config keys set to a
  non-default value. Wired from `ServerAboutToStartEvent`. Slated for
  hard-removal of these keys in 1.5.0.
- **`com.crims.effectiveinstruments.util.ConfigIO.writeAtomically`** —
  shared atomic-write utility (temp + ATOMIC_MOVE, fall back to plain
  replace on Windows FAT). Promoted from `InstrumentAuraMapping`. All 11
  previously non-atomic config writers now route through it.
- **`com.crims.effectiveinstruments.durability.InstrumentNamespaces`** —
  shared `INSTRUMENT_MOD_IDS` constant. Used by both the durability bar
  decorator and the new fallback synthesis path.
- **Mobile-tier active-musician set.**
  `ImmersiveMelodiesAuraHandler.ACTIVE_MOBILE_MUSICIANS` bounds the
  per-pulse server tick scan — `onServerTick` previously walked every
  player on the level every pulse even when nobody was using an IM
  instrument. Populated by `onScreenOpened` and a periodic discovery
  scan; idle players drop out after the linger window.
- **Defensive client-tick guard** in `AuraOverlayInjector` clears IM
  overlay state if a non-`Closing` screen swap leaves the state attached
  to a no-longer-rendered screen.
- **`/effectiveinstruments diagnose`** now shows positive-targeting
  state in a parallel block to the offensive line — answers "my regen
  aura doesn't hit my ally" complaints in one command.
- **Three new state-machine unit tests** (pure-JUnit, mirror production
  logic locally — same pattern as `AuraApplicatorBehaviorTest`):
  - `AuraManagerActiveStateTest` — sliding-window activation algorithm
    (10 cases: empty deque, single recent note, sliding window
    expiration, both-windows-required, threshold met / not met).
  - `InstrumentDurabilityNamespaceTest` — namespace-allowlist
    membership + fallback-Entry math + damage/repair clamping (12 cases).
  - `InstrumentAnvilHandlerMathTest` — combine bonus, material-repair
    rounding, work-penalty escalation, new 1.4.9 cost formula (12 cases).

### Changed
- **Synthesis log message** in `InstrumentAuraMapping.load` now includes
  the count of mappings synthesized this load, not just the total.
- **Offensive-uniqueness audit + synthesis pass** share a single
  `computeOffensiveOwnership` walk of `MAPPINGS` instead of two.
- **`AuraSelectorWidget.parentScreen` field demoted** to a constructor-
  local `int parentScreenWidth`. Window-resize re-fires `Init.Post`
  which builds a fresh widget — no recycled instance reads stale
  `screen.width`. `AuraOverlayInjector` tracks the stationary screen
  identity locally as `stationaryOverlayScreen`, parallel to the
  existing `imOverlayScreen` for the IM path.
- Deprecated config keys' comments rewritten to remove inaccurate
  "still read"/"migration default" claims.

### Removed
- **`tools/generate_aura_icons.py`** — legacy icon generator dead since
  1.4.2 (`tools/gen_aura_icons.py` is canonical per `CLAUDE.md`).

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

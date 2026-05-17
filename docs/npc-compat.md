# Effective Instruments — NPC compatibility (v1.6.0)

v1.6.0 generalizes the aura source from "a player" to "any LivingEntity" via the
`IAuraPerformer` abstraction. Once a supported NPC mod is installed, the
matching adapter wakes up at common-setup and the mod's NPCs can drive the
aura pipeline.

This document covers, per mod: install requirement, what the adapter does,
the config keys, and troubleshooting.

## How adapters work

Each adapter ships under `compat/<modid>/`. At common-setup, EI's
`PerformerRegistry.discover()` walks `META-INF/services/...$PerformerAdapterProvider`
and calls `bootstrap()` on every provider whose mod is detected via
`ModList.isLoaded`. The adapter:

1. **Wraps** the mod's entity class via `Class.forName` + cached `MethodHandle`s (no compile-time dep on closed-source mods).
2. **Registers** `OwnerProvider` / `FactionProvider` instances with the cross-mod resolvers.
3. **Subscribes** to `EntityJoinLevelEvent` (Forge bus) and injects a `PlayInstrumentGoal` subclass into the NPC's `goalSelector`.

If the target mod isn't installed the adapter sits dormant — zero overhead, no log spam.

**Diagnostics:**
- `/effectiveinstruments npcs adapters` — list active adapters
- `/effectiveinstruments npcs list [radius]` — list wrappable NPCs near you
- `/effectiveinstruments npcs diagnose <entity>` — dump one NPC's performer state

## Tier-1 adapters (full performer)

### Recruits (`recruits`)
- **Install:** Recruits 1.15.0+ for Forge 1.20.1 (talhanation/recruits).
- **What works:** Recruits play held instruments when idle (no target, not fleeing, not in raid). Aura applies in a 12-block radius (16 × 0.75 default multiplier).
- **Owner model:** `getOwnerUUID()` — same-owner recruits classify as `OWN_PET`.
- **Config:** `npcs.recruits.{enabled, allowPerformers, allowOffensiveAuras, respectRecruitsFaction}`.
- **Combat veto:** target acquired, took damage, fleeing flag, `should_rest` flag, or `aggro_state == AGGRO_RAID`.
- **Troubleshooting:** if a recruit never plays, run `/effectiveinstruments npcs diagnose <recruit>` and check `canPerformNow`. Most common cause: `requireOwnerOnline=true` (default) and the owner is offline.

### Guard Villagers (`guardvillagers`)
- **Install:** Guard Villagers 1.6.15+ for Forge 1.20.1 (seymourimadeit/guardvillagers).
- **What works:** Guards play when idle. Hero-of-the-Village player within 24 blocks is the implicit owner.
- **Owner model:** `getOwnerId()` direct, fallback to nearby Hero-of-the-Village player.
- **Config:** `npcs.guardvillagers.{enabled, allowPerformers, heroPlayerRadius}`.

### Easy NPC (`easy_npc`)
- **Install:** Easy NPC 5.0+ for Forge 1.20.1 (MarkusBordihn/BOs-Easy-NPC).
- **What works:** All `EasyNPC` marker-interface implementers can perform.
- **Owner model:** `OwnableEntity` (handled by vanilla path).
- **Config:** `npcs.easy_npc.{enabled, allowPerformers}`.
- **Caveat:** profession-swap rebuilds the NPC's goalSelector. The goal is re-injected on the next `EntityJoinLevelEvent`; if a swap happens without an entity rejoin, restart the NPC or run `/effectiveinstruments reload`.

### Doggy Talents Next (`doggytalents`)
- **Install:** Doggy Talents Next 1.18.64+ for Forge 1.20.1 (DashieDev/DoggyTalentsNext).
- **What works:** Dogs play when sitting OR in docile mode. Instrument lives in the **OFFHAND** slot.
- **Owner model:** vanilla `TamableAnimal`.
- **Config:** `npcs.doggytalents.{enabled, allowPerformers, requireSittingOrDocile}`.

### Iron's Spells 'n Spellbooks (`irons_spellbooks`)
- **Install:** Iron's Spells 3.4+ for Forge 1.20.1 (iron431/irons-spells-n-spellbooks).
- **What works:** Summons (skeleton, zombie, polar bear, vex) play between combats. Stops 60 ticks before despawn (timer-bound).
- **Owner model:** `MagicSummon.getSummoner()` returns the summoner LivingEntity; we read its UUID.
- **Config:** `npcs.irons_spellbooks.{enabled, allowPerformers, minSummonTimerRemaining}`.

### Ars Nouveau Starbuncle (`ars_nouveau`)
- **Install:** Ars Nouveau 4.0+ for Forge 1.20.1 (baileyholl/Ars-Nouveau).
- **What works:** Starbuncle plays when idle (no target).
- **Owner model:** **ownerless** — Starbuncle has no tracked owner UUID. Plays for nearby targets via the global classifier (no friend/foe split based on tamer).
- **Config:** `npcs.ars_nouveau.{enabled, allowStarbuncle, allowFamiliars}` (familiars deferred to 1.7.0).

### Touhou Little Maid (`touhou_little_maid`)
- **Install:** Touhou Little Maid 1.5.0+ for Forge 1.20.1 (TartaricAcid/TouhouLittleMaid).
- **What works:** Maids play during non-combat tasks. Goal-based (not Brain-based — EntityMaid extends TamableAnimal so it has both).
- **Owner model:** vanilla `TamableAnimal`.
- **Combat veto:** maid's `IMaidTask.getUid()` is checked against `attack`/`combat`/`guard` substrings.
- **Config:** `npcs.touhou_little_maid.{enabled, allowPerformers, allowOffensiveAuras=false}`. Maids are buff-only by default per mod theme.

### MCA Reborn (`mca`)
- **Install:** MCA Reborn 7.0+ for Forge 1.20.1 (Luke100000/minecraft-comes-alive).
- **What works (1.6.0 hotfix #5 — Tier-1 promotion):** MCA villagers can BOTH receive auras AND perform them. Hand a villager an instrument via `/replaceitem entity <selector> weapon.mainhand <instrument_id>` — within 60 ticks she'll play it and apply the instrument's default aura. Married villagers also continue to be classified as `OWN_PET` of their spouse when the spouse plays an instrument.
- **Owner model:** `EntityRelationship.of(villager).getPartnerUUID()` returns the spouse UUID, treated as the villager's owner.
- **Combat veto:** target acquired, recent damage, sleeping (`AbstractVillager.isSleeping`), trading (`Villager.isTrading`). Goal priority 8 — sits below combat / profession goals.
- **Config:** `npcs.mca.{enabled, respectRelationships}`.
- **Diagnosing:** `/effectiveinstruments npcs list 16` includes MCA villagers as performers now. For target-side classification of villagers, use `/effectiveinstruments npcs diagnose @e[type=mca:villager,limit=1]` — when she's not wrappable as a performer (e.g., goal mid-cooldown), the diagnose command shows her resolved owner, JSON override, and how she'd be classified as a target.

## Library hooks

### Pehkui (`pehkui`)
- **Install:** Pehkui 3.0+ for Forge 1.20.1 (Virtuoel/Pehkui).
- **What works:** A non-player performer's aura radius is multiplied by its Pehkui `BASE` scale. Player path is never scaled (preserves 1.5.0 parity).
- **Config:** `npcs.pehkui.respectScaleForRadius`.

## Tag-driven overrides (modpack authors)

Three data tags short-circuit the classifier:

- `effective_instruments:always_buff` — force `OWN_PET` (positive aura always applies)
- `effective_instruments:always_debuff` — force `HOSTILE_MOB` (offensive aura always applies)
- `effective_instruments:ignore` — force `HOSTILE_MOB` (skipped by positive polarity)

Plus two adapter-control tags:

- `effective_instruments:force_performer` — lift an entity into performer eligibility (assumes mainhand instrument detection)
- `effective_instruments:never_performer` — veto performer status even when an adapter matches

Default tag JSONs live under `data/effective_instruments/tags/entity_types/` and ship empty. Modpack datapacks can override them.

## JSON-driven overrides (server admins)

`config/effective_instruments/entity_classification.json` is created on first run with curated defaults for ~22 entities. Format per row:

```json
"alexsmobs:gorilla": { "category": "OWN_PET", "requireTamed": true }
```

- `category` — one of `MUSICIAN | OWN_PET | OTHER_PLAYER | OTHER_PLAYER_PET | VILLAGER | IRON_GOLEM | PASSIVE_MOB | HOSTILE_MOB`
- `requireTamed` — when true, only applies to tamed instances (vanilla `TamableAnimal` / `AbstractHorse`)
- `delegateTo` — reserved for cross-mod hand-off (1.7.0+)

Tag overrides win over JSON; JSON wins over adapter defaults; adapter defaults win over vanilla classification.

Reload after edits: `/effectiveinstruments reload`.

## Audit invariant

EI's `checkCompatAuditInvariant` gradle task enforces that classes outside `compat/<modid>/` never import the mod's runtime types. Run `./gradlew check` to verify; fails the build on violation.

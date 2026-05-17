package com.crims.effectiveinstruments.config;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class EIServerConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue NOTE_WINDOW_TICKS;
    public static final ForgeConfigSpec.IntValue AURA_TICK_INTERVAL;
    public static final ForgeConfigSpec.IntValue DEFAULT_RADIUS;
    public static final ForgeConfigSpec.EnumValue<OverwritePolicy> EFFECT_OVERWRITE_POLICY;
    public static final ForgeConfigSpec.IntValue NOTE_THRESHOLD_MIN;
    public static final ForgeConfigSpec.IntValue NOTE_THRESHOLD_WINDOW_TICKS;
    public static final ForgeConfigSpec.BooleanValue DEBUG_MODE;
    // OverwritePolicy itself lives at com.crims.effectiveinstruments.config.OverwritePolicy

    public static final ForgeConfigSpec.BooleanValue ALLOW_SELF_BUFF;
    public static final ForgeConfigSpec.BooleanValue INCLUDE_OTHER_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue INCLUDE_TAMED_PETS;
    public static final ForgeConfigSpec.IntValue MAX_TARGETS_PER_TICK;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PET_ENTITY_ALLOWLIST;

    // Per-polarity category toggles (added in 1.4.1). Musician + own-pet inclusion
    // is polarity-enforced and never read from config; these cover every other
    // category. See com.crims.effectiveinstruments.aura.EntityCategory.
    public static final ForgeConfigSpec.BooleanValue POSITIVE_INCLUDE_OTHER_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue POSITIVE_INCLUDE_OTHER_PLAYER_PETS;
    public static final ForgeConfigSpec.BooleanValue POSITIVE_INCLUDE_VILLAGERS;
    public static final ForgeConfigSpec.BooleanValue POSITIVE_INCLUDE_IRON_GOLEMS;
    public static final ForgeConfigSpec.BooleanValue POSITIVE_INCLUDE_PASSIVE_MOBS;
    public static final ForgeConfigSpec.BooleanValue POSITIVE_INCLUDE_HOSTILE_MOBS;
    public static final ForgeConfigSpec.IntValue POSITIVE_MAX_TARGETS_PER_TICK;

    public static final ForgeConfigSpec.BooleanValue OFFENSIVE_INCLUDE_OTHER_PLAYER_PETS;
    public static final ForgeConfigSpec.BooleanValue OFFENSIVE_INCLUDE_VILLAGERS;
    public static final ForgeConfigSpec.BooleanValue OFFENSIVE_INCLUDE_IRON_GOLEMS;
    public static final ForgeConfigSpec.BooleanValue OFFENSIVE_INCLUDE_PASSIVE_MOBS;
    public static final ForgeConfigSpec.BooleanValue OFFENSIVE_INCLUDE_HOSTILE_MOBS;

    // Mobile tier (Immersive Melodies passive buffs) — added in 1.3.0.
    public static final ForgeConfigSpec.BooleanValue MOBILE_TIER_ENABLED;
    public static final ForgeConfigSpec.IntValue MOBILE_PULSE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue MOBILE_LINGER_TICKS;
    public static final ForgeConfigSpec.IntValue MOBILE_DEFAULT_RADIUS;
    public static final ForgeConfigSpec.IntValue MOBILE_MAX_TARGETS_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue MOBILE_ALLOW_SELF_BUFF;
    public static final ForgeConfigSpec.BooleanValue MOBILE_INCLUDE_OTHER_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue MOBILE_INCLUDE_TAMED_PETS;
    public static final ForgeConfigSpec.BooleanValue SUPPRESS_MOBILE_WHEN_STATIONARY_ACTIVE;

    // Offensive auras + durability (added in 1.4.0).
    public static final ForgeConfigSpec.BooleanValue OFFENSIVE_AURAS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue OFFENSIVE_INCLUDE_ALL_NON_PETS;
    public static final ForgeConfigSpec.BooleanValue OFFENSIVE_ALLOW_SELF;
    public static final ForgeConfigSpec.BooleanValue OFFENSIVE_INCLUDE_OTHER_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue OFFENSIVE_INCLUDE_TAMED_PETS;
    public static final ForgeConfigSpec.IntValue OFFENSIVE_MAX_TARGETS_PER_TICK;
    public static final ForgeConfigSpec.IntValue OFFENSIVE_DURABILITY_COST_MULT;

    public static final ForgeConfigSpec.BooleanValue DURABILITY_ENABLED;
    public static final ForgeConfigSpec.BooleanValue DURABILITY_CREATIVE_IMMUNITY;
    public static final ForgeConfigSpec.IntValue DURABILITY_COST_PER_NOTE;
    public static final ForgeConfigSpec.IntValue DURABILITY_COST_PER_MOBILE_PULSE;
    public static final ForgeConfigSpec.IntValue DURABILITY_DEFAULT_MAX;
    public static final ForgeConfigSpec.IntValue DURABILITY_ANVIL_REPAIR_BONUS_PERCENT;

    // 1.6.0: NPC performer framework master toggles. Empty config block emits
    // no per-mod sections — those land in [npcs.<modid>] subsections (one per
    // adapter shipping in Phases 2-5).
    public static final ForgeConfigSpec.BooleanValue NPCS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NPCS_ALLOW_PERFORMERS;
    public static final ForgeConfigSpec.BooleanValue NPCS_ALLOW_TARGETS;
    public static final ForgeConfigSpec.IntValue NPCS_MAX_PERFORMERS_PER_CHUNK;
    public static final ForgeConfigSpec.IntValue NPCS_MAX_PERFORMERS_PER_OWNER;
    public static final ForgeConfigSpec.IntValue NPCS_MAX_PERFORMERS_PER_SERVER;
    public static final ForgeConfigSpec.IntValue NPCS_PERFORMER_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue NPCS_PERFORMER_SCAN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue NPCS_OWNER_NEARBY_RADIUS;
    public static final ForgeConfigSpec.DoubleValue NPCS_PERFORMER_AURA_RADIUS_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue NPCS_REQUIRE_OWNER_ONLINE;
    public static final ForgeConfigSpec.BooleanValue NPCS_RESPECT_SCOREBOARD_TEAMS;
    public static final ForgeConfigSpec.IntValue NPCS_MAX_TARGETS_PER_TICK_PER_PERFORMER;

    // [npcs.recruits] — Phase 2.
    public static final ForgeConfigSpec.BooleanValue NPCS_RECRUITS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NPCS_RECRUITS_ALLOW_PERFORMERS;
    public static final ForgeConfigSpec.BooleanValue NPCS_RECRUITS_ALLOW_OFFENSIVE_AURAS;
    public static final ForgeConfigSpec.BooleanValue NPCS_RECRUITS_RESPECT_FACTION;

    // [npcs.guardvillagers] — Phase 3.
    public static final ForgeConfigSpec.BooleanValue NPCS_GUARDVILLAGERS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NPCS_GUARDVILLAGERS_ALLOW_PERFORMERS;
    public static final ForgeConfigSpec.IntValue NPCS_GUARDVILLAGERS_HERO_RADIUS;

    // [npcs.easy_npc] — Phase 3b.
    public static final ForgeConfigSpec.BooleanValue NPCS_EASY_NPC_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NPCS_EASY_NPC_ALLOW_PERFORMERS;

    // [npcs.doggytalents] — Phase 3b.
    public static final ForgeConfigSpec.BooleanValue NPCS_DOGGYTALENTS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NPCS_DOGGYTALENTS_ALLOW_PERFORMERS;
    public static final ForgeConfigSpec.BooleanValue NPCS_DOGGYTALENTS_REQUIRE_SITTING_OR_DOCILE;

    // [npcs.irons_spellbooks] — Phase 3b.
    public static final ForgeConfigSpec.BooleanValue NPCS_IRONS_SPELLBOOKS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NPCS_IRONS_SPELLBOOKS_ALLOW_PERFORMERS;
    public static final ForgeConfigSpec.IntValue NPCS_IRONS_SPELLBOOKS_MIN_TIMER_REMAINING;

    // [npcs.ars_nouveau] — Phase 3b (Starbuncle Tier 1, ownerless).
    public static final ForgeConfigSpec.BooleanValue NPCS_ARS_NOUVEAU_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NPCS_ARS_NOUVEAU_ALLOW_STARBUNCLE;
    public static final ForgeConfigSpec.BooleanValue NPCS_ARS_NOUVEAU_ALLOW_FAMILIARS;

    // [npcs.touhou_little_maid] — Phase 4.
    public static final ForgeConfigSpec.BooleanValue NPCS_TOUHOU_LITTLE_MAID_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NPCS_TOUHOU_LITTLE_MAID_ALLOW_PERFORMERS;
    public static final ForgeConfigSpec.BooleanValue NPCS_TOUHOU_LITTLE_MAID_ALLOW_OFFENSIVE_AURAS;

    // [npcs.mca] — Phase 4 (Tier 2, target-only).
    public static final ForgeConfigSpec.BooleanValue NPCS_MCA_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NPCS_MCA_RESPECT_RELATIONSHIPS;

    // [npcs.pehkui] — Phase 5 library hook.
    public static final ForgeConfigSpec.BooleanValue NPCS_PEHKUI_RESPECT_SCALE_FOR_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General settings").push("general");
        ENABLED = builder
                .comment("Master enable/disable for all aura effects")
                .define("enabled", true);
        NOTE_WINDOW_TICKS = builder
                .comment("Ticks after last note before aura deactivates. 100 ticks = 5 seconds.")
                .defineInRange("noteWindowTicks", 100, 20, 600);
        AURA_TICK_INTERVAL = builder
                .comment("How often (in ticks) to refresh aura effects on targets")
                .defineInRange("auraTickIntervalTicks", 10, 1, 100);
        DEFAULT_RADIUS = builder
                .comment("Default radius in blocks for aura effect range (used when an aura's radius is -1)")
                .defineInRange("defaultRadius", 16, 1, 64);
        EFFECT_OVERWRITE_POLICY = builder
                .comment(
                        "How aura effects interact with pre-existing effects on targets.",
                        "NEVER_OVERWRITE: only apply when the target has no effect of this type.",
                        "STRONGER_ONLY:   overwrite only if the new amplifier is strictly greater.",
                        "REFRESH_TIES:    overwrite if new amplifier >= existing (refreshes duration on ties).",
                        "ALWAYS:          always overwrite regardless of existing amplifier.")
                .defineEnum("effectOverwritePolicy", OverwritePolicy.REFRESH_TIES);
        NOTE_THRESHOLD_MIN = builder
                .comment(
                        "Minimum number of notes the player must play within the threshold window",
                        "before an aura becomes active. Set to 1 to preserve legacy behavior,",
                        "or higher (e.g. 3) to prevent 'hold one note' exploits.")
                .defineInRange("noteThresholdMin", 1, 1, 32);
        NOTE_THRESHOLD_WINDOW_TICKS = builder
                .comment("Size of the sliding window (in ticks) in which notes are counted for noteThresholdMin.")
                .defineInRange("noteThresholdWindowTicks", 40, 1, 600);
        DEBUG_MODE = builder
                .comment("Emit per-tick diagnostics (active musician count, scanned entities, tick time). Noisy — keep off in production.")
                .define("debugMode", false);
        builder.pop();

        builder.comment(
                        "Legacy target selection settings (pre-1.4.1). Kept for backwards-compat with",
                        "existing server.toml files. The four toggles below are *ignored at runtime*.",
                        "If any is set to a non-default value on startup the mod logs a one-shot",
                        "deprecation warning. Slated for removal in 1.5.0.")
                .push("targeting");
        ALLOW_SELF_BUFF = builder
                .comment("Deprecated since 1.4.1. Ignored at runtime; emits a deprecation warning on startup if non-default.")
                .define("allowSelfBuff", true);
        INCLUDE_OTHER_PLAYERS = builder
                .comment("Deprecated since 1.4.1. Ignored at runtime; emits a deprecation warning on startup if non-default. Replacement: positiveTargeting.includeOtherPlayers.")
                .define("includeOtherPlayers", true);
        INCLUDE_TAMED_PETS = builder
                .comment("Deprecated since 1.4.1. Ignored at runtime; emits a deprecation warning on startup if non-default.")
                .define("includeTamedPets", true);
        MAX_TARGETS_PER_TICK = builder
                .comment("Deprecated since 1.4.1. Ignored at runtime; emits a deprecation warning on startup if non-default. Replacement: positiveTargeting.maxTargetsPerTick.")
                .defineInRange("maxTargetsPerTick", 32, 1, 512);
        PET_ENTITY_ALLOWLIST = builder
                .comment("Additional entity type IDs to treat as 'pets' beyond TamableAnimal/AbstractHorse. Still in effect.")
                .defineListAllowEmpty(
                        "petEntityTypeAllowlist",
                        List::of,
                        obj -> obj instanceof String s && ResourceLocation.isValidResourceLocation(s)
                );
        builder.pop();

        builder.comment(
                        "Positive (support) aura targeting — 1.4.1+.",
                        "The musician and their own tamed pets are ALWAYS included and cannot be",
                        "toggled here. Every other category is opt-in.")
                .push("positiveTargeting");
        POSITIVE_INCLUDE_OTHER_PLAYERS = builder
                .comment("Include other players in range.")
                .define("includeOtherPlayers", true);
        POSITIVE_INCLUDE_OTHER_PLAYER_PETS = builder
                .comment("Include other players' tamed pets.")
                .define("includeOtherPlayerPets", true);
        POSITIVE_INCLUDE_VILLAGERS = builder
                .comment("Include villagers and wandering traders.")
                .define("includeVillagers", true);
        POSITIVE_INCLUDE_IRON_GOLEMS = builder
                .comment("Include iron golems.")
                .define("includeIronGolems", true);
        POSITIVE_INCLUDE_PASSIVE_MOBS = builder
                .comment("Include passive mobs (cows, sheep, fish, etc.). Off by default — positive aura on a cow pen is usually noise.")
                .define("includePassiveMobs", false);
        POSITIVE_INCLUDE_HOSTILE_MOBS = builder
                .comment("Include hostile mobs. Off by default — Strength/Regen on a zombie is a trap.")
                .define("includeHostileMobs", false);
        POSITIVE_MAX_TARGETS_PER_TICK = builder
                .comment(
                        "Hard cap on how many entities a positive aura affects per tick.",
                        "Targets beyond the cap are silently dropped in category priority order",
                        "(musician → own pets → other players → other players' pets → villagers → iron golems → passive → hostile).")
                .defineInRange("maxTargetsPerTick", 32, 1, 512);
        builder.pop();

        builder.comment(
                        "Mobile tier — passive buffs while a player holds a playing Immersive Melodies instrument.",
                        "No-ops when Immersive Melodies is not installed.")
                .push("mobileTier");
        MOBILE_TIER_ENABLED = builder
                .comment("Master enable/disable for the mobile tier")
                .define("enabled", true);
        MOBILE_PULSE_INTERVAL_TICKS = builder
                .comment(
                        "How often (in ticks) to refresh mobile-tier effects.",
                        "Higher = cheaper but laggier reactions.",
                        "20 ticks = 1 second.")
                .defineInRange("pulseIntervalTicks", 20, 5, 100);
        MOBILE_LINGER_TICKS = builder
                .comment("How long (in ticks) mobile effects linger after the player stops playing.")
                .defineInRange("lingerTicks", 60, 0, 200);
        MOBILE_DEFAULT_RADIUS = builder
                .comment("Default radius in blocks for mobile-tier auras whose JSON radius is -1.")
                .defineInRange("defaultRadius", 8, 1, 32);
        MOBILE_MAX_TARGETS_PER_TICK = builder
                .comment("Hard cap on buffed entities per mobile-tier musician per pulse.")
                .defineInRange("maxTargetsPerTick", 16, 1, 256);
        MOBILE_ALLOW_SELF_BUFF = builder
                .comment("Deprecated since 1.4.1. Ignored at runtime; emits a deprecation warning on startup if non-default. Mobile targeting now derives from positiveTargeting/offensiveTargeting via TargetingProfiles.")
                .define("allowSelfBuff", true);
        MOBILE_INCLUDE_OTHER_PLAYERS = builder
                .comment("Deprecated since 1.4.1. Ignored at runtime; emits a deprecation warning on startup if non-default. Replacement: positiveTargeting.includeOtherPlayers.")
                .define("includeOtherPlayers", true);
        MOBILE_INCLUDE_TAMED_PETS = builder
                .comment("Deprecated since 1.4.1. Ignored at runtime; emits a deprecation warning on startup if non-default.")
                .define("includeTamedPets", false);
        SUPPRESS_MOBILE_WHEN_STATIONARY_ACTIVE = builder
                .comment("If true, a player's active stationary aura suppresses the mobile tier for that player.")
                .define("suppressWhenStationaryActive", true);
        builder.pop();

        builder.comment(
                        "Offensive (debuff) aura targeting — 1.4.1+.",
                        "The musician and their own tamed pets are ALWAYS excluded and cannot be",
                        "toggled here. Every other category is opt-in.")
                .push("offensiveTargeting");
        OFFENSIVE_AURAS_ENABLED = builder
                .comment("Master enable/disable for offensive (negative-polarity) auras. When false, negative presets are never applied.")
                .define("enabled", true);
        OFFENSIVE_INCLUDE_ALL_NON_PETS = builder
                .comment(
                        "When true (default), offensive auras affect EVERY living entity in range",
                        "except the musician, the musician's own pets, and other players' pets —",
                        "ignoring the individual include* knobs below. This matches the 'hit all mobs",
                        "except pets' intent. Set to false to fall back to fine-grained per-category",
                        "control via the include* knobs.")
                .define("includeAllNonPets", true);
        OFFENSIVE_ALLOW_SELF = builder
                .comment("Deprecated since 1.4.1. Ignored at runtime; emits a deprecation warning on startup if non-default. Offensive auras never hit the musician.")
                .define("allowSelf", false);
        OFFENSIVE_INCLUDE_OTHER_PLAYERS = builder
                .comment("Include other players as offensive-aura targets (PvP friendly-fire).")
                .define("includeOtherPlayers", true);
        OFFENSIVE_INCLUDE_TAMED_PETS = builder
                .comment("Deprecated since 1.4.1. Ignored at runtime; emits a deprecation warning on startup if non-default. Replacement: offensiveTargeting.includeOtherPlayerPets. Own pets are always skipped.")
                .define("includeTamedPets", false);
        OFFENSIVE_INCLUDE_OTHER_PLAYER_PETS = builder
                .comment("Include other players' tamed pets as offensive-aura targets. Off by default — friendly-fire on someone else's wolf is usually accidental.")
                .define("includeOtherPlayerPets", false);
        OFFENSIVE_INCLUDE_VILLAGERS = builder
                .comment("Include villagers and wandering traders as offensive-aura targets. Off by default.")
                .define("includeVillagers", false);
        OFFENSIVE_INCLUDE_IRON_GOLEMS = builder
                .comment("Include iron golems as offensive-aura targets. Off by default — golems near villagers will aggro the player.")
                .define("includeIronGolems", false);
        OFFENSIVE_INCLUDE_PASSIVE_MOBS = builder
                .comment("Include passive mobs (cows, sheep, fish, etc.) as offensive-aura targets. Off by default.")
                .define("includePassiveMobs", false);
        OFFENSIVE_INCLUDE_HOSTILE_MOBS = builder
                .comment("Include hostile mobs as offensive-aura targets. On by default — this is the primary intent.")
                .define("includeHostileMobs", true);
        OFFENSIVE_MAX_TARGETS_PER_TICK = builder
                .comment("Hard cap on entities an offensive aura can hit per tick.")
                .defineInRange("maxTargetsPerTick", 32, 1, 512);
        OFFENSIVE_DURABILITY_COST_MULT = builder
                .comment("Durability cost multiplier applied on top of durabilityCostPerNote when a NEGATIVE aura is active. 2 = offensive play wears the instrument twice as fast.")
                .defineInRange("durabilityCostMultiplier", 2, 1, 8);
        builder.pop();

        builder.comment(
                        "Instrument durability — per-instrument 'health' tracked in custom NBT (1.4.0+).",
                        "Per-instrument max durability and repair materials live in",
                        "config/effective_instruments/instrument_durability.json.")
                .push("durability");
        DURABILITY_ENABLED = builder
                .comment("Master enable/disable for the durability system. When false, tooltip / damage / broken-state gating all no-op.")
                .define("enabled", true);
        DURABILITY_CREATIVE_IMMUNITY = builder
                .comment(
                        "When true, creative-mode players' instruments don't take durability damage",
                        "(matches vanilla tool behavior). Set to false to verify durability depletion",
                        "while testing in creative.")
                .define("creativeImmunity", true);
        DURABILITY_COST_PER_NOTE = builder
                .comment("Durability points consumed per note played (stationary tier, positive auras).")
                .defineInRange("costPerNote", 1, 1, 8);
        DURABILITY_COST_PER_MOBILE_PULSE = builder
                .comment("Durability points consumed per successful mobile-tier pulse (1 per pulseIntervalTicks).")
                .defineInRange("costPerMobilePulse", 1, 1, 8);
        DURABILITY_DEFAULT_MAX = builder
                .comment("Fallback maximum durability for instruments missing from instrument_durability.json.")
                .defineInRange("defaultMax", 1200, 1, 100_000);
        DURABILITY_ANVIL_REPAIR_BONUS_PERCENT = builder
                .comment("Anvil 'combine two damaged copies' bonus, as a percentage of max durability. Vanilla tools use 12%.")
                .defineInRange("anvilCombineBonusPercent", 12, 0, 100);
        builder.pop();

        // 1.6.0: NPC performer framework. Master toggles + per-mod sub-blocks.
        builder.comment(
                        "NPC performer framework (1.6.0+). Lets supported NPC mods (Recruits,",
                        "Guard Villagers, Touhou Maids, Iron's Spells summons, etc.) drive the aura",
                        "pipeline. Each adapter is gated by ModList.isLoaded(<modid>) — if you don't",
                        "have the target mod installed the section is inert.")
                .push("npcs");
        NPCS_ENABLED = builder
                .comment("Master switch for the entire NPC framework. Set false to disable all NPC performers + target classification.")
                .define("enabled", true);
        NPCS_ALLOW_PERFORMERS = builder
                .comment("Allow NPCs to act as aura sources. Set false to keep NPCs as targets only.")
                .define("allowPerformers", true);
        NPCS_ALLOW_TARGETS = builder
                .comment("Allow NPCs to receive aura effects. Set false to make NPCs invisible to the aura system entirely.")
                .define("allowTargets", true);
        NPCS_MAX_PERFORMERS_PER_CHUNK = builder
                .comment("Performance gate: max simultaneous NPC performers within any single chunk.")
                .defineInRange("maxPerformersPerChunk", 4, 1, 64);
        NPCS_MAX_PERFORMERS_PER_OWNER = builder
                .comment("Performance gate: max simultaneous NPC performers owned by any single player.")
                .defineInRange("maxPerformersPerOwner", 8, 1, 256);
        NPCS_MAX_PERFORMERS_PER_SERVER = builder
                .comment("Performance gate: hard upper bound on simultaneous NPC performers server-wide.")
                .defineInRange("maxPerformersPerServer", 256, 1, 4096);
        NPCS_PERFORMER_COOLDOWN_TICKS = builder
                .comment("Default cooldown between NPC performances. Per-mod sub-blocks may override.")
                .defineInRange("performerCooldownTicks", 60, 1, 6000);
        NPCS_PERFORMER_SCAN_INTERVAL_TICKS = builder
                .comment("How often (in ticks) the NPC framework re-evaluates eligibility for active performers.")
                .defineInRange("performerScanIntervalTicks", 20, 1, 200);
        NPCS_OWNER_NEARBY_RADIUS = builder
                .comment("Distance in blocks within which an NPC's owner must be present for the NPC to be considered 'supervised'.")
                .defineInRange("ownerNearbyRadius", 32, 4, 128);
        NPCS_PERFORMER_AURA_RADIUS_MULTIPLIER = builder
                .comment("Multiplier applied to the aura radius when an NPC (not a player) is the performer.")
                .defineInRange("performerAuraRadiusMultiplier", 0.75, 0.1, 4.0);
        NPCS_REQUIRE_OWNER_ONLINE = builder
                .comment("If true, NPCs only perform when their owner UUID resolves to an online player.")
                .define("requireOwnerOnline", true);
        NPCS_RESPECT_SCOREBOARD_TEAMS = builder
                .comment("If true, scoreboard team membership is consulted as part of friend/foe classification for NPC performers and targets.")
                .define("respectScoreboardTeams", true);
        NPCS_MAX_TARGETS_PER_TICK_PER_PERFORMER = builder
                .comment("Max aura targets any single NPC performer can affect per tick. Bounds armies-of-NPCs scenarios.")
                .defineInRange("maxTargetsPerTickPerPerformer", 16, 1, 256);

        builder.comment("Recruits (talhanation/recruits) — Phase 2 of 1.6.0 NPC compat.").push("recruits");
        NPCS_RECRUITS_ENABLED = builder
                .comment("Master switch for the Recruits adapter. Inert when Recruits is absent at runtime.")
                .define("enabled", true);
        NPCS_RECRUITS_ALLOW_PERFORMERS = builder
                .comment("Allow recruits to perform auras when idle (no target, not fleeing/resting/in-raid).")
                .define("allowPerformers", true);
        NPCS_RECRUITS_ALLOW_OFFENSIVE_AURAS = builder
                .comment("Allow recruits to perform negative-polarity auras. Set false for buff-only recruit musicians.")
                .define("allowOffensiveAuras", true);
        NPCS_RECRUITS_RESPECT_FACTION = builder
                .comment("Consult Recruits' RecruitsDiplomacyManager for ALLY/ENEMY classification of other recruits.")
                .define("respectRecruitsFaction", true);
        builder.pop(); // npcs.recruits

        builder.comment("Guard Villagers (tallestegg/guardvillagers) — Phase 3.").push("guardvillagers");
        NPCS_GUARDVILLAGERS_ENABLED = builder
                .comment("Master switch for the Guard Villagers adapter.")
                .define("enabled", true);
        NPCS_GUARDVILLAGERS_ALLOW_PERFORMERS = builder
                .comment("Allow guards to perform auras while not in combat.")
                .define("allowPerformers", true);
        NPCS_GUARDVILLAGERS_HERO_RADIUS = builder
                .comment("Distance in blocks within which a HERO_OF_THE_VILLAGE player counts as an implicit guard owner.")
                .defineInRange("heroPlayerRadius", 24, 4, 64);
        builder.pop(); // npcs.guardvillagers

        builder.comment("Easy NPC (MarkusBordihn/BOs-Easy-NPC) — Phase 3.").push("easy_npc");
        NPCS_EASY_NPC_ENABLED = builder.define("enabled", true);
        NPCS_EASY_NPC_ALLOW_PERFORMERS = builder.define("allowPerformers", true);
        builder.pop(); // npcs.easy_npc

        builder.comment("Doggy Talents Next (DashieDev/DoggyTalentsNext) — Phase 3.").push("doggytalents");
        NPCS_DOGGYTALENTS_ENABLED = builder.define("enabled", true);
        NPCS_DOGGYTALENTS_ALLOW_PERFORMERS = builder.define("allowPerformers", true);
        NPCS_DOGGYTALENTS_REQUIRE_SITTING_OR_DOCILE = builder
                .comment("If true, dogs only play when sitting or in docile mode.")
                .define("requireSittingOrDocile", true);
        builder.pop(); // npcs.doggytalents

        builder.comment("Iron's Spells 'n Spellbooks (iron431/irons-spells-n-spellbooks) — Phase 3.").push("irons_spellbooks");
        NPCS_IRONS_SPELLBOOKS_ENABLED = builder.define("enabled", true);
        NPCS_IRONS_SPELLBOOKS_ALLOW_PERFORMERS = builder.define("allowPerformers", true);
        NPCS_IRONS_SPELLBOOKS_MIN_TIMER_REMAINING = builder
                .comment("Minimum ticks of remaining summon-timer effect required for a summon to start playing.")
                .defineInRange("minSummonTimerRemaining", 60, 0, 6000);
        builder.pop(); // npcs.irons_spellbooks

        builder.comment("Ars Nouveau (baileyholl/Ars-Nouveau) — Phase 3 (Starbuncle Tier 1; familiars Tier 2 deferred).").push("ars_nouveau");
        NPCS_ARS_NOUVEAU_ENABLED = builder.define("enabled", true);
        NPCS_ARS_NOUVEAU_ALLOW_STARBUNCLE = builder
                .comment("Allow Starbuncles to perform auras. Ownerless — plays for any nearby targets.")
                .define("allowStarbuncle", true);
        NPCS_ARS_NOUVEAU_ALLOW_FAMILIARS = builder
                .comment("Reserved for Phase 5 — promote Ars Nouveau familiars to first-class targets. No effect today.")
                .define("allowFamiliars", false);
        builder.pop(); // npcs.ars_nouveau

        builder.comment("Touhou Little Maid (TartaricAcid/TouhouLittleMaid) — Phase 4.").push("touhou_little_maid");
        NPCS_TOUHOU_LITTLE_MAID_ENABLED = builder.define("enabled", true);
        NPCS_TOUHOU_LITTLE_MAID_ALLOW_PERFORMERS = builder.define("allowPerformers", true);
        NPCS_TOUHOU_LITTLE_MAID_ALLOW_OFFENSIVE_AURAS = builder
                .comment("Allow maids to perform negative-polarity auras. Default false — maids are buff-only by mod theme.")
                .define("allowOffensiveAuras", false);
        builder.pop(); // npcs.touhou_little_maid

        builder.comment("Minecraft Comes Alive (Luke100000/minecraft-comes-alive) — Phase 4 (Tier 2 target-only).").push("mca");
        NPCS_MCA_ENABLED = builder.define("enabled", true);
        NPCS_MCA_RESPECT_RELATIONSHIPS = builder
                .comment("Honor MCA marriage/relationship state — spouse is treated as the villager's owner for aura classification.")
                .define("respectRelationships", true);
        builder.pop(); // npcs.mca

        builder.comment("Pehkui (Virtuoel/Pehkui) — Phase 5 library hook for non-player aura-radius scaling.").push("pehkui");
        NPCS_PEHKUI_RESPECT_SCALE_FOR_RADIUS = builder
                .comment("Multiply non-player performer aura radius by their Pehkui BASE scale. Player path is never scaled.")
                .define("respectScaleForRadius", true);
        builder.pop(); // npcs.pehkui

        builder.pop(); // npcs

        SPEC = builder.build();
    }

    /**
     * Safe accessor for {@link #DURABILITY_ENABLED} on the client render path.
     * Forge SERVER-config values are not loaded on the title screen, world list,
     * or any item-preview screen rendered before joining a world; calling
     * {@code .get()} there throws {@link IllegalStateException}. Use this from
     * any client-side reader that may run pre-join — failure mode is "feature
     * silently off until config arrives via handshake."
     */
    public static boolean isDurabilityEnabledSafe() {
        try {
            return DURABILITY_ENABLED.get();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    /**
     * One-shot startup pass that warns when any of the deprecated 1.4.x config
     * keys are set to a non-default value. Wired from the
     * {@code ServerAboutToStartEvent} handler in {@code EffectiveInstrumentsMod}
     * so the SERVER spec is loaded before we read. Slated for hard-removal in
     * 1.5.0; emitting the warning gives admins a window to clean their TOMLs.
     */
    public static void warnDeprecated() {
        warnIfNonDefault("targeting.allowSelfBuff",                 ALLOW_SELF_BUFF.get(),                 true);
        warnIfNonDefault("targeting.includeOtherPlayers",           INCLUDE_OTHER_PLAYERS.get(),           true);
        warnIfNonDefault("targeting.includeTamedPets",              INCLUDE_TAMED_PETS.get(),              true);
        warnIfNonDefault("targeting.maxTargetsPerTick",             MAX_TARGETS_PER_TICK.get(),            32);
        warnIfNonDefault("offensiveTargeting.allowSelf",            OFFENSIVE_ALLOW_SELF.get(),            false);
        warnIfNonDefault("offensiveTargeting.includeTamedPets",     OFFENSIVE_INCLUDE_TAMED_PETS.get(),    false);
        warnIfNonDefault("mobileTier.allowSelfBuff",                MOBILE_ALLOW_SELF_BUFF.get(),          true);
        warnIfNonDefault("mobileTier.includeOtherPlayers",          MOBILE_INCLUDE_OTHER_PLAYERS.get(),    true);
        warnIfNonDefault("mobileTier.includeTamedPets",             MOBILE_INCLUDE_TAMED_PETS.get(),       false);
    }

    private static void warnIfNonDefault(String key, Object actual, Object specDefault) {
        if (!java.util.Objects.equals(actual, specDefault)) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Config key '{}' is deprecated and ignored at runtime (current value: {}). "
                            + "Remove it from server.toml — slated for hard-removal in 1.5.0.",
                    key, actual);
        }
    }

    private EIServerConfig() {}
}

package com.crims.effectiveinstruments.config;

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
                        "existing server.toml files. 'allowSelfBuff' and 'includeTamedPets' are now",
                        "*ignored at runtime* — positive auras always cover the musician and their own",
                        "pets, negative auras never do. A one-shot warning is logged on startup if",
                        "either is set to the no-longer-honored value.")
                .push("targeting");
        ALLOW_SELF_BUFF = builder
                .comment("DEPRECATED in 1.4.1 — ignored at runtime. Positive auras always include the musician.")
                .define("allowSelfBuff", true);
        INCLUDE_OTHER_PLAYERS = builder
                .comment("DEPRECATED in 1.4.1 — moved to positiveTargeting.includeOtherPlayers. Still read as a migration default on first boot.")
                .define("includeOtherPlayers", true);
        INCLUDE_TAMED_PETS = builder
                .comment("DEPRECATED in 1.4.1 — ignored at runtime. Positive auras always include the musician's own pets.")
                .define("includeTamedPets", true);
        MAX_TARGETS_PER_TICK = builder
                .comment(
                        "DEPRECATED in 1.4.1 — moved to positiveTargeting.maxTargetsPerTick.",
                        "Still honored as a fallback cap when the positive block is at its default.")
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
                .comment("Whether the mobile-tier musician receives their own effects")
                .define("allowSelfBuff", true);
        MOBILE_INCLUDE_OTHER_PLAYERS = builder
                .comment("Whether other players in range receive mobile-tier effects")
                .define("includeOtherPlayers", true);
        MOBILE_INCLUDE_TAMED_PETS = builder
                .comment("Whether tamed animals in range receive mobile-tier effects (off by default — passive, weaker tier)")
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
                .comment("DEPRECATED in 1.4.1 — ignored at runtime. Offensive auras never hit the musician.")
                .define("allowSelf", false);
        OFFENSIVE_INCLUDE_OTHER_PLAYERS = builder
                .comment("Include other players as offensive-aura targets (PvP friendly-fire).")
                .define("includeOtherPlayers", true);
        OFFENSIVE_INCLUDE_TAMED_PETS = builder
                .comment("DEPRECATED in 1.4.1 — replaced by offensiveTargeting.includeOtherPlayerPets. Own pets are always skipped. Read for a one-shot migration warning.")
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

        SPEC = builder.build();
    }

    private EIServerConfig() {}
}

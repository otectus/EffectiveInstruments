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

        builder.comment("Target selection settings").push("targeting");
        ALLOW_SELF_BUFF = builder
                .comment("Whether the musician receives their own aura effects")
                .define("allowSelfBuff", true);
        INCLUDE_OTHER_PLAYERS = builder
                .comment("Whether other players in range receive aura effects")
                .define("includeOtherPlayers", true);
        INCLUDE_TAMED_PETS = builder
                .comment("Whether tamed animals in range receive aura effects")
                .define("includeTamedPets", true);
        MAX_TARGETS_PER_TICK = builder
                .comment(
                        "Hard cap on how many entities a single musician can buff per tick.",
                        "Protects against performance cliffs in crowded farms.",
                        "Targets beyond the cap are silently dropped in insertion order (self → players → pets).")
                .defineInRange("maxTargetsPerTick", 32, 1, 512);
        PET_ENTITY_ALLOWLIST = builder
                .comment("Additional entity type IDs to treat as 'pets' beyond TamableAnimal/AbstractHorse")
                .defineListAllowEmpty(
                        "petEntityTypeAllowlist",
                        List::of,
                        obj -> obj instanceof String s && ResourceLocation.isValidResourceLocation(s)
                );
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

        SPEC = builder.build();
    }

    private EIServerConfig() {}
}

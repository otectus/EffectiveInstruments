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

    public static final ForgeConfigSpec.BooleanValue ALLOW_SELF_BUFF;
    public static final ForgeConfigSpec.BooleanValue INCLUDE_OTHER_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue INCLUDE_TAMED_PETS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PET_ENTITY_ALLOWLIST;

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
        PET_ENTITY_ALLOWLIST = builder
                .comment("Additional entity type IDs to treat as 'pets' beyond TamableAnimal/AbstractHorse")
                .defineListAllowEmpty(
                        "petEntityTypeAllowlist",
                        List::of,
                        obj -> obj instanceof String s && ResourceLocation.isValidResourceLocation(s)
                );
        builder.pop();

        SPEC = builder.build();
    }

    private EIServerConfig() {}
}

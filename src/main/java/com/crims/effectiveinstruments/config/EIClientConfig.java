package com.crims.effectiveinstruments.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class EIClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SHOW_OVERLAY;
    public static final ForgeConfigSpec.DoubleValue OVERLAY_SCALE;
    public static final ForgeConfigSpec.BooleanValue COMPACT_MODE;
    public static final ForgeConfigSpec.EnumValue<ParticlesMode> PARTICLES_MODE;
    public static final ForgeConfigSpec.BooleanValue REDUCED_MOTION;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCREEN_CLASS_ALLOWLIST;

    public enum ParticlesMode {
        ALL, MINIMAL, NONE
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Overlay settings for instrument screens").push("overlay");
        SHOW_OVERLAY = builder
                .comment("Show aura selector overlay on instrument screens")
                .define("showOverlay", true);
        OVERLAY_SCALE = builder
                .comment("Scale factor for the overlay buttons")
                .defineInRange("overlayScale", 1.0, 0.5, 2.0);
        COMPACT_MODE = builder
                .comment("Use compact button layout (smaller icons)")
                .define("compactMode", false);
        builder.pop();

        builder.comment("Screen detection settings").push("screens");
        SCREEN_CLASS_ALLOWLIST = builder
                .comment(
                        "Fallback: fully-qualified class names of screens to treat as instrument screens.",
                        "Used for instrument mods that do NOT extend Genshin Instruments' InstrumentScreen.",
                        "Genshin Instruments and Even More Instruments screens are detected automatically."
                )
                .defineListAllowEmpty(
                        "screenClassAllowlist",
                        List::of,
                        obj -> obj instanceof String
                );
        builder.pop();

        builder.comment("Visual effects settings").push("visuals");
        PARTICLES_MODE = builder
                .comment("Particle effects when aura is active: ALL, MINIMAL, or NONE")
                .defineEnum("particlesMode", ParticlesMode.ALL);
        REDUCED_MOTION = builder
                .comment(
                        "Reduce aura particle motion (dampens velocity and pulse intensity).",
                        "Useful for players sensitive to rapid visual motion.")
                .define("reducedMotion", false);
        builder.pop();

        SPEC = builder.build();
    }

    private EIClientConfig() {}
}

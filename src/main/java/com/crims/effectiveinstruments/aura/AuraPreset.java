package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.config.EIServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import javax.annotation.Nullable;
import java.util.List;

public record AuraPreset(
        String id,
        Component displayName,
        Component description,
        int color,
        List<EffectEntry> effects,
        int defaultDurationTicks,
        int radiusOverride,
        boolean enabled,
        int sortOrder,
        @Nullable ResourceLocation iconTexture,
        @Nullable ResourceLocation selectedIconTexture
) {
    public record EffectEntry(
            MobEffect effect,
            int amplifier
    ) {}

    /**
     * Returns the effective radius for this aura.
     * If the JSON specifies a non-negative radius (0+), use it; otherwise fall back to the global TOML default.
     */
    public int getEffectiveRadius() {
        if (radiusOverride >= 0) {
            return radiusOverride;
        }
        return EIServerConfig.DEFAULT_RADIUS.get();
    }

    /**
     * Returns the effect duration. The JSON value is the single source of truth.
     */
    public int getEffectiveDuration() {
        return defaultDurationTicks;
    }
}

package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.config.EIServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
        @Nullable ResourceLocation selectedIconTexture,
        Set<BuffTier> supportedTiers,
        boolean showInSelector
) {
    public record EffectEntry(
            MobEffect effect,
            int amplifier
    ) {}

    /**
     * Returns the effective radius for this aura.
     * If the JSON specifies a non-negative radius (0+), use it; otherwise fall back to the global TOML default.
     * Note: the global default is the <b>stationary</b> radius; the mobile tick handler
     * resolves {@code -1} against its own {@code MOBILE_DEFAULT_RADIUS} instead of calling
     * this helper.
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

    /** True if this preset may be applied from the given tier. */
    public boolean supports(BuffTier tier) {
        return supportedTiers.contains(tier);
    }

    /**
     * Back-compat factory used by loaders that haven't been taught about tiers yet (notably
     * {@link AuraJsonLoader} when a JSON file omits the {@code tiers} field). Defaults to a
     * stationary-only, selector-visible preset — matches pre-1.3.0 behavior exactly.
     */
    public static AuraPreset stationaryOnly(
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
        return new AuraPreset(
                id, displayName, description, color, effects,
                defaultDurationTicks, radiusOverride, enabled, sortOrder,
                iconTexture, selectedIconTexture,
                EnumSet.of(BuffTier.STATIONARY),
                true
        );
    }
}

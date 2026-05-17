package com.crims.effectiveinstruments.compat.recruits;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.IAuraPerformer;
import com.crims.effectiveinstruments.performer.PerformerRegistry;
import com.crims.effectiveinstruments.performer.PerformerTier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.util.EnumSet;
import java.util.Optional;

/**
 * ServiceLoader entry for the Recruits adapter. Discovered at common-setup
 * by {@link PerformerRegistry#discover()} via
 * {@code META-INF/services/com.crims.effectiveinstruments.performer.PerformerRegistry$PerformerAdapterProvider}.
 *
 * <p>{@link #bootstrap(IEventBus)} delegates to
 * {@link RecruitsCompat#initCommon()} so the Forge bus subscription, owner
 * provider, and faction provider all register from one place. Idempotent:
 * RecruitsCompat short-circuits on the second call.
 */
public final class RecruitPerformerAdapter implements PerformerRegistry.PerformerAdapterProvider {

    @Override public String modId() { return RecruitsCompat.MODID; }

    @Override public boolean isLoaded() { return ModList.get().isLoaded(modId()); }

    @Override public void bootstrap(IEventBus modBus) {
        RecruitsCompat.initCommon();
    }

    @Override public Optional<IAuraPerformer> wrap(LivingEntity e, PerformerTier requested) {
        if (!RecruitsCompat.isAvailable()) return Optional.empty();
        // Safety gate: NPC framework globally enabled?
        try {
            if (!EIServerConfig.NPCS_ENABLED.get()) return Optional.empty();
            if (!EIServerConfig.NPCS_RECRUITS_ENABLED.get()) return Optional.empty();
        } catch (IllegalStateException ignored) {
            // SERVER config not yet loaded — happens during very-early lookups.
            // Conservatively skip rather than panic.
            return Optional.empty();
        }
        if (!RecruitsReflection.isRecruit(e)) return Optional.empty();
        return Optional.of(new RecruitPerformer(e, requested));
    }

    @Override public EnumSet<PerformerRegistry.Capability> capabilities() {
        return EnumSet.of(
                PerformerRegistry.Capability.STATIONARY_PLAY,
                PerformerRegistry.Capability.MOBILE_PLAY,
                PerformerRegistry.Capability.AURA_TARGET,
                PerformerRegistry.Capability.OWNER_AWARE);
    }
}

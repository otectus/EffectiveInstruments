package com.crims.effectiveinstruments.compat.mca;

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
 * MCA Reborn ServiceLoader entry.
 *
 * <p>1.6.0 hotfix #5: promoted from Tier-2 (target-only) to Tier-1
 * (full performer). Villagers wrap as {@link MCAPerformer} and play
 * instruments via {@link MCAInstrumentGoal} injected at
 * {@code EntityJoinLevelEvent}. The {@link MCAOwnerProvider} continues to
 * route spouse-as-owner classification for villagers who are TARGETS of
 * other performers.
 */
public final class MCAPerformerAdapter implements PerformerRegistry.PerformerAdapterProvider {

    @Override public String modId() { return MCACompat.MODID; }
    @Override public boolean isLoaded() { return ModList.get().isLoaded(modId()); }
    @Override public void bootstrap(IEventBus modBus) { MCACompat.initCommon(); }

    @Override public Optional<IAuraPerformer> wrap(LivingEntity e, PerformerTier requested) {
        if (!MCACompat.isAvailable()) return Optional.empty();
        try {
            if (!EIServerConfig.NPCS_ENABLED.get()) return Optional.empty();
        } catch (IllegalStateException ignored) { return Optional.empty(); }
        if (!MCAReflection.isMcaVillager(e)) return Optional.empty();
        return Optional.of(new MCAPerformer(e, requested));
    }

    @Override public EnumSet<PerformerRegistry.Capability> capabilities() {
        return EnumSet.of(
                PerformerRegistry.Capability.STATIONARY_PLAY,
                PerformerRegistry.Capability.MOBILE_PLAY,
                PerformerRegistry.Capability.AURA_TARGET,
                PerformerRegistry.Capability.OWNER_AWARE);
    }
}

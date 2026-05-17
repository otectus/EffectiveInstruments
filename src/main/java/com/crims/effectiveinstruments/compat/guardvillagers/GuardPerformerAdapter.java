package com.crims.effectiveinstruments.compat.guardvillagers;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.IAuraPerformer;
import com.crims.effectiveinstruments.performer.PerformerRegistry;
import com.crims.effectiveinstruments.performer.PerformerTier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.util.EnumSet;
import java.util.Optional;

public final class GuardPerformerAdapter implements PerformerRegistry.PerformerAdapterProvider {

    @Override public String modId() { return GuardVillagersCompat.MODID; }
    @Override public boolean isLoaded() { return ModList.get().isLoaded(modId()); }
    @Override public void bootstrap(IEventBus modBus) { GuardVillagersCompat.initCommon(); }

    @Override public Optional<IAuraPerformer> wrap(LivingEntity e, PerformerTier requested) {
        if (!GuardVillagersCompat.isAvailable()) return Optional.empty();
        try {
            if (!EIServerConfig.NPCS_ENABLED.get()) return Optional.empty();
        } catch (IllegalStateException ignored) {
            return Optional.empty();
        }
        if (!GuardVillagersReflection.isGuard(e)) return Optional.empty();
        return Optional.of(new GuardPerformer(e, requested));
    }

    @Override public EnumSet<PerformerRegistry.Capability> capabilities() {
        return EnumSet.of(
                PerformerRegistry.Capability.STATIONARY_PLAY,
                PerformerRegistry.Capability.MOBILE_PLAY,
                PerformerRegistry.Capability.AURA_TARGET,
                PerformerRegistry.Capability.OWNER_AWARE);
    }
}

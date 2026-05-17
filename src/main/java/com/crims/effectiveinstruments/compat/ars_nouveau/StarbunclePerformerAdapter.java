package com.crims.effectiveinstruments.compat.ars_nouveau;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.IAuraPerformer;
import com.crims.effectiveinstruments.performer.PerformerRegistry;
import com.crims.effectiveinstruments.performer.PerformerTier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.util.EnumSet;
import java.util.Optional;

public final class StarbunclePerformerAdapter implements PerformerRegistry.PerformerAdapterProvider {

    @Override public String modId() { return ArsNouveauCompat.MODID; }
    @Override public boolean isLoaded() { return ModList.get().isLoaded(modId()); }
    @Override public void bootstrap(IEventBus modBus) { ArsNouveauCompat.initCommon(); }

    @Override public Optional<IAuraPerformer> wrap(LivingEntity e, PerformerTier requested) {
        if (!ArsNouveauCompat.isAvailable()) return Optional.empty();
        try {
            if (!EIServerConfig.NPCS_ENABLED.get()) return Optional.empty();
        } catch (IllegalStateException ignored) { return Optional.empty(); }
        if (!ArsNouveauReflection.isStarbuncle(e)) return Optional.empty();
        return Optional.of(new StarbunclePerformer(e, requested));
    }

    @Override public EnumSet<PerformerRegistry.Capability> capabilities() {
        // No OWNER_AWARE because Starbuncle has no owner concept.
        return EnumSet.of(
                PerformerRegistry.Capability.STATIONARY_PLAY,
                PerformerRegistry.Capability.MOBILE_PLAY,
                PerformerRegistry.Capability.AURA_TARGET);
    }
}

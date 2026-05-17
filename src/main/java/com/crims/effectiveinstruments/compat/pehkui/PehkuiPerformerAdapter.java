package com.crims.effectiveinstruments.compat.pehkui;

import com.crims.effectiveinstruments.performer.IAuraPerformer;
import com.crims.effectiveinstruments.performer.PerformerRegistry;
import com.crims.effectiveinstruments.performer.PerformerTier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.util.EnumSet;
import java.util.Optional;

/**
 * ServiceLoader entry for the Pehkui library hook. {@link #wrap} always
 * returns empty — Pehkui isn't a performer, it's a radius modifier. The
 * {@link #bootstrap} call is what wires the modifier into the global
 * registry.
 */
public final class PehkuiPerformerAdapter implements PerformerRegistry.PerformerAdapterProvider {

    @Override public String modId() { return PehkuiCompat.MODID; }
    @Override public boolean isLoaded() { return ModList.get().isLoaded(modId()); }
    @Override public void bootstrap(IEventBus modBus) { PehkuiCompat.initCommon(); }

    @Override public Optional<IAuraPerformer> wrap(LivingEntity e, PerformerTier requested) {
        return Optional.empty(); // library, not a performer
    }

    @Override public EnumSet<PerformerRegistry.Capability> capabilities() {
        return EnumSet.noneOf(PerformerRegistry.Capability.class);
    }
}

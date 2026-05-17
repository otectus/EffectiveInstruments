package com.crims.effectiveinstruments.compat.doggytalents;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class DoggyTalentsCompat {

    public static final String MODID = "doggytalents";

    private static boolean available = false;
    private static boolean initialized = false;

    public static synchronized void initCommon() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded(MODID)) {
            EffectiveInstrumentsMod.LOGGER.debug("EI Doggy Talents Next adapter: mod absent");
            return;
        }
        if (!DoggyTalentsReflection.tryResolve()) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "EI Doggy Talents Next adapter: ModList reports loaded but reflection failed; inert");
            return;
        }
        available = true;
        MinecraftForge.EVENT_BUS.register(DoggyTalentsEventHandler.class);
        // Owner via TamableAnimal (vanilla path) — no custom provider.
        EffectiveInstrumentsMod.LOGGER.info("EI Doggy Talents Next adapter active");
    }

    public static boolean isAvailable() { return available; }

    private DoggyTalentsCompat() {}
}

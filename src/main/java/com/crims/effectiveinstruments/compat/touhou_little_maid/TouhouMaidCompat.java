package com.crims.effectiveinstruments.compat.touhou_little_maid;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class TouhouMaidCompat {

    public static final String MODID = "touhou_little_maid";

    private static boolean available = false;
    private static boolean initialized = false;

    public static synchronized void initCommon() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded(MODID)) {
            EffectiveInstrumentsMod.LOGGER.debug("EI Touhou Little Maid adapter: mod absent");
            return;
        }
        if (!TouhouMaidReflection.tryResolve()) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "EI Touhou Little Maid adapter: ModList reports loaded but reflection failed; inert");
            return;
        }
        available = true;
        MinecraftForge.EVENT_BUS.register(TouhouMaidEventHandler.class);
        // Owner via vanilla TamableAnimal — no custom provider.
        EffectiveInstrumentsMod.LOGGER.info("EI Touhou Little Maid adapter active");
    }

    public static boolean isAvailable() { return available; }

    private TouhouMaidCompat() {}
}

package com.crims.effectiveinstruments.compat.ars_nouveau;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class ArsNouveauCompat {

    public static final String MODID = "ars_nouveau";

    private static boolean available = false;
    private static boolean initialized = false;

    public static synchronized void initCommon() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded(MODID)) {
            EffectiveInstrumentsMod.LOGGER.debug("EI Ars Nouveau adapter: mod absent");
            return;
        }
        if (!ArsNouveauReflection.tryResolve()) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "EI Ars Nouveau adapter: ModList reports loaded but reflection failed; inert");
            return;
        }
        available = true;
        MinecraftForge.EVENT_BUS.register(ArsNouveauEventHandler.class);
        // No OwnerProvider — Starbuncle has no tracked owner UUID.
        EffectiveInstrumentsMod.LOGGER.info("EI Ars Nouveau adapter active (Starbuncle, ownerless)");
    }

    public static boolean isAvailable() { return available; }

    private ArsNouveauCompat() {}
}

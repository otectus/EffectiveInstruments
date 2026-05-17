package com.crims.effectiveinstruments.compat.guardvillagers;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.performer.OwnerResolver;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

/** Bootstrap for the Guard Villagers adapter. Mirrors {@code RecruitsCompat}. */
public final class GuardVillagersCompat {

    public static final String MODID = "guardvillagers";

    private static boolean available = false;
    private static boolean initialized = false;

    public static synchronized void initCommon() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded(MODID)) {
            EffectiveInstrumentsMod.LOGGER.debug("EI Guard Villagers adapter: mod absent");
            return;
        }
        if (!GuardVillagersReflection.tryResolve()) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "EI Guard Villagers adapter: ModList reports loaded but reflection failed; adapter inert");
            return;
        }
        available = true;
        MinecraftForge.EVENT_BUS.register(GuardVillagersEventHandler.class);
        OwnerResolver.register(new GuardVillagersOwnerProvider());
        EffectiveInstrumentsMod.LOGGER.info("EI Guard Villagers adapter active");
    }

    public static boolean isAvailable() { return available; }

    private GuardVillagersCompat() {}
}

package com.crims.effectiveinstruments.compat.irons_spellbooks;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.performer.OwnerResolver;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class IronsSpellbooksCompat {

    public static final String MODID = "irons_spellbooks";

    private static boolean available = false;
    private static boolean initialized = false;

    public static synchronized void initCommon() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded(MODID)) {
            EffectiveInstrumentsMod.LOGGER.debug("EI Iron's Spells adapter: mod absent");
            return;
        }
        if (!IronsSpellbooksReflection.tryResolve()) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "EI Iron's Spells adapter: ModList reports loaded but reflection failed; inert");
            return;
        }
        available = true;
        MinecraftForge.EVENT_BUS.register(IronsSpellbooksEventHandler.class);
        OwnerResolver.register(new IronsSpellbooksOwnerProvider());
        EffectiveInstrumentsMod.LOGGER.info("EI Iron's Spells adapter active");
    }

    public static boolean isAvailable() { return available; }

    private IronsSpellbooksCompat() {}
}

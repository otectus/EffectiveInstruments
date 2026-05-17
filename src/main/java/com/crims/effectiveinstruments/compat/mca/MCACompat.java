package com.crims.effectiveinstruments.compat.mca;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.performer.OwnerResolver;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class MCACompat {

    public static final String MODID = "mca";

    private static boolean available = false;
    private static boolean initialized = false;

    public static synchronized void initCommon() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded(MODID)) {
            EffectiveInstrumentsMod.LOGGER.debug("EI MCA Reborn adapter: mod absent");
            return;
        }
        if (!MCAReflection.tryResolve()) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "EI MCA Reborn adapter: ModList reports loaded but reflection failed; inert");
            return;
        }
        available = true;
        OwnerResolver.register(new MCAOwnerProvider());
        // 1.6.0 hotfix #5: Tier-1 promotion — wire the goal-injection
        // EntityJoinLevelEvent listener so villagers play instruments.
        MinecraftForge.EVENT_BUS.register(MCAEventHandler.class);
        EffectiveInstrumentsMod.LOGGER.info("EI MCA Reborn adapter active (Tier-1 performer)");
    }

    public static boolean isAvailable() { return available; }

    private MCACompat() {}
}

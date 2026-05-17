package com.crims.effectiveinstruments.compat.easy_npc;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class EasyNpcCompat {

    public static final String MODID = "easy_npc";

    private static boolean available = false;
    private static boolean initialized = false;

    public static synchronized void initCommon() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded(MODID)) {
            EffectiveInstrumentsMod.LOGGER.debug("EI Easy NPC adapter: mod absent");
            return;
        }
        if (!EasyNpcReflection.tryResolve()) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "EI Easy NPC adapter: ModList reports loaded but reflection failed; inert");
            return;
        }
        available = true;
        MinecraftForge.EVENT_BUS.register(EasyNpcEventHandler.class);
        // Owner resolution is via OwnableEntity (vanilla) — no provider needed.
        EffectiveInstrumentsMod.LOGGER.info("EI Easy NPC adapter active");
    }

    public static boolean isAvailable() { return available; }

    private EasyNpcCompat() {}
}

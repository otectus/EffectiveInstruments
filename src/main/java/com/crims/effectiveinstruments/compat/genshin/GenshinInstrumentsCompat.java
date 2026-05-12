package com.crims.effectiveinstruments.compat.genshin;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

/**
 * Quarantined bootstrap for Genshin Instruments. <b>Imports no GI classes</b>
 * — the only coupling is the mod id string. When GI is present we register
 * {@link GenshinInstrumentEventHandler} manually on the Forge event bus; when
 * GI is absent we simply skip registration so the GI-typed listener methods
 * never get linked, sidestepping {@code NoClassDefFoundError} at dispatch time.
 *
 * <p>This is the GI counterpart of
 * {@link com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesCompat}.
 * As of 1.5.0 GI is an optional backend on the same conceptual footing as IM:
 * either, both, or neither may be installed.
 */
public final class GenshinInstrumentsCompat {

    /** Mod id as declared in Genshin Instruments' {@code mods.toml}. */
    public static final String MODID = "genshinstrument";

    /**
     * Set once at common-setup. Defaults to {@code false} so reads before
     * {@link #initCommon()} runs return "absent" rather than throwing.
     */
    private static boolean available = false;

    public static void initCommon() {
        available = ModList.get().isLoaded(MODID);
        if (available) {
            EffectiveInstrumentsMod.LOGGER.info(
                    "Genshin Instruments detected — stationary tier compat enabled");
            // Manual class-level registration: static @SubscribeEvent methods
            // on the handler. This is the only point where the handler class is
            // touched, so its GI imports never resolve when GI is absent.
            MinecraftForge.EVENT_BUS.register(GenshinInstrumentEventHandler.class);
        } else {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "Genshin Instruments not present — stationary tier compat inactive");
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    private GenshinInstrumentsCompat() {}
}

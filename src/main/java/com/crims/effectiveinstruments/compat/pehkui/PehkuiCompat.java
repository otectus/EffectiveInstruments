package com.crims.effectiveinstruments.compat.pehkui;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.performer.PerformerRadiusModifier;
import net.minecraftforge.fml.ModList;

/**
 * Pehkui library hook. Registers a {@link PerformerRadiusModifier} that
 * multiplies the aura radius by the performer's Pehkui {@code BASE} scale.
 * Non-player performers only — the player path stays byte-identical to 1.5.0
 * regardless of whether the player is scaled, per spec §6.14.
 */
public final class PehkuiCompat {

    public static final String MODID = "pehkui";

    private static boolean available = false;
    private static boolean initialized = false;

    public static synchronized void initCommon() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded(MODID)) {
            EffectiveInstrumentsMod.LOGGER.debug("EI Pehkui adapter: mod absent");
            return;
        }
        if (!PehkuiReflection.tryResolve()) {
            return;
        }
        available = true;
        PerformerRadiusModifier.Registry.register((performer, base) ->
                base * PehkuiReflection.getBaseScale(performer.entity()));
        EffectiveInstrumentsMod.LOGGER.info("EI Pehkui adapter active (radius scales with BASE)");
    }

    public static boolean isAvailable() { return available; }

    private PehkuiCompat() {}
}

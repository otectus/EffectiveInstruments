package com.crims.effectiveinstruments.compat.recruits;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.performer.FactionResolver;
import com.crims.effectiveinstruments.performer.OwnerResolver;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

/**
 * Bootstrap for the Recruits adapter. Mirrors {@code GenshinInstrumentsCompat}:
 * <ul>
 *   <li>{@link #initCommon()} idempotent — detects mod presence via
 *       {@link ModList#isLoaded(String)} and short-circuits when absent.</li>
 *   <li>Registers {@link RecruitsEventHandler} on the Forge bus only when
 *       Recruits is present, so a missing mod imposes zero overhead.</li>
 *   <li>Registers {@link RecruitOwnerProvider} + {@link RecruitFactionProvider}
 *       with the cross-mod resolvers so other adapters can ask "is this
 *       entity a Recruits soldier?" uniformly.</li>
 * </ul>
 *
 * <p>Called from {@link com.crims.effectiveinstruments.performer.PerformerRegistry#bootstrapAll}
 * via {@link RecruitPerformerAdapter#bootstrap}.
 */
public final class RecruitsCompat {

    public static final String MODID = "recruits";

    private static boolean available = false;
    private static boolean initialized = false;

    public static synchronized void initCommon() {
        if (initialized) return;
        initialized = true;

        if (!ModList.get().isLoaded(MODID)) {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "EI Recruits adapter: Recruits mod not present, skipping bootstrap");
            return;
        }
        if (!RecruitsReflection.tryResolve()) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "EI Recruits adapter: ModList reports loaded but reflection failed to resolve "
                            + "AbstractRecruitEntity. Mod-version drift suspected; adapter inert.");
            return;
        }
        available = true;

        MinecraftForge.EVENT_BUS.register(RecruitsEventHandler.class);
        OwnerResolver.register(new RecruitOwnerProvider());
        FactionResolver.register(new RecruitFactionProvider());

        EffectiveInstrumentsMod.LOGGER.info("EI Recruits adapter active");
    }

    public static boolean isAvailable() { return available; }

    private RecruitsCompat() {}
}

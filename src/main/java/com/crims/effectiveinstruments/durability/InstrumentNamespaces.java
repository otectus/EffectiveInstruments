package com.crims.effectiveinstruments.durability;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Single source of truth for "which mod ids count as instruments" for the
 * durability + decoration paths. Lifted out of
 * {@link com.crims.effectiveinstruments.client.event.InstrumentDurabilityBarDecorator}
 * in 1.4.9 so the decorator AND
 * {@link InstrumentDurability#isLikelyInstrument(ResourceLocation)} share
 * the same allowlist — adding a new instrument-mod namespace happens in one
 * place rather than two.
 */
public final class InstrumentNamespaces {

    /**
     * Mod ids whose items are treated as instruments by default. Items in any
     * other namespace are NOT eligible for the durability default-max fallback
     * (see {@link InstrumentDurability#getMax}) and do NOT receive a durability
     * bar decoration. Modpack authors who want third-party instruments tracked
     * should add an explicit entry to {@code instrument_durability.json} —
     * which always wins over namespace gating.
     */
    public static final Set<String> INSTRUMENT_MOD_IDS = Set.of(
            "genshinstrument",
            "evenmoreinstruments",
            "immersive_melodies"
    );

    public static boolean contains(ResourceLocation id) {
        return id != null && INSTRUMENT_MOD_IDS.contains(id.getNamespace());
    }

    private InstrumentNamespaces() {}
}

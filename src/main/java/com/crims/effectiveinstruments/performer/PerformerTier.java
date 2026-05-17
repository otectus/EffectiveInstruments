package com.crims.effectiveinstruments.performer;

/**
 * Which side of the aura pipeline a performer is currently driving. STATIONARY
 * = Genshin Instruments / Even More Instruments (event-driven note delivery,
 * subscribed via {@code GenshinInstrumentEventHandler}). MOBILE = Immersive
 * Melodies (per-pulse scan with NBT and screen-open activation paths). Kept
 * separate from {@code BuffTier} which is a preset-side concept.
 */
public enum PerformerTier {
    STATIONARY,
    MOBILE
}

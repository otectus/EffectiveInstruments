package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;

import java.util.*;

public final class AuraRegistry {
    private static final Map<String, AuraPreset> PRESETS = new LinkedHashMap<>();
    private static final List<String> ENABLED_IDS = new ArrayList<>();
    private static List<AuraPreset> cachedEnabledPresets = List.of();
    private static boolean loaded = false;

    public static void load() {
        PRESETS.clear();
        ENABLED_IDS.clear();

        AuraJsonLoader.ensureDefaults();
        List<AuraPreset> all = AuraJsonLoader.loadAll();
        for (AuraPreset preset : all) {
            PRESETS.put(preset.id(), preset);
            if (preset.enabled()) {
                ENABLED_IDS.add(preset.id());
            }
        }
        cachedEnabledPresets = ENABLED_IDS.stream()
                .map(PRESETS::get)
                .filter(Objects::nonNull)
                .toList();
        loaded = true;
        EffectiveInstrumentsMod.LOGGER.info("Loaded {} aura presets ({} enabled)",
                PRESETS.size(), ENABLED_IDS.size());

        InstrumentAuraMapping.ensureDefaults();
        InstrumentAuraMapping.load();

        MobileInstrumentAuraMapping.ensureDefaults();
        MobileInstrumentAuraMapping.load();

        AuraApplicator.invalidatePetAllowlistCache();
    }

    public static void reload() {
        load();
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static Collection<AuraPreset> getAllPresets() {
        return Collections.unmodifiableCollection(PRESETS.values());
    }

    public static Optional<AuraPreset> getById(String id) {
        return Optional.ofNullable(PRESETS.get(id));
    }

    public static List<AuraPreset> getEnabledPresets() {
        return cachedEnabledPresets;
    }

    private AuraRegistry() {}
}

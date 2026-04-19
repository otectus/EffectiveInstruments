package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.config.InstrumentDurabilityConfig;
import net.minecraftforge.registries.ForgeRegistries;

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
        // Self-heal any offensive presets that got lost between installs (partial
        // first-run, manual deletion, etc.). If any are regenerated, reload so
        // they're picked up in this same call.
        List<String> healed = AuraJsonLoader.healMissingOffensivePresets();
        List<AuraPreset> all = AuraJsonLoader.loadAll();
        if (!healed.isEmpty()) {
            // Already regenerated above; loadAll just ran over them too.
        }
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

        // 1.4.4 uniqueness guard. Warn-level only so user-custom duplicates
        // don't hard-fail the boot, but shipped defaults are expected to pass.
        auditUniqueness();

        InstrumentAuraMapping.ensureDefaults();
        InstrumentAuraMapping.load();

        MobileInstrumentAuraMapping.ensureDefaults();
        MobileInstrumentAuraMapping.load();

        InstrumentDurabilityConfig.ensureDefaults();
        InstrumentDurabilityConfig.load();
        com.crims.effectiveinstruments.durability.InstrumentDurability.invalidateEntryCache();

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

    /**
     * Walk every loaded preset and report duplicate
     * {@code (polarity, sorted_effects, sorted_amplifiers)} tuples.
     * Shipped defaults are expected to be unique — when two presets collide,
     * log them at warn-level so the issue is visible in the mod log without
     * preventing boot (user-customs can legitimately shadow each other).
     */
    private static void auditUniqueness() {
        Map<String, List<String>> byTuple = new HashMap<>();
        for (AuraPreset preset : PRESETS.values()) {
            String key = tupleKey(preset);
            byTuple.computeIfAbsent(key, k -> new ArrayList<>()).add(preset.id());
        }
        int duplicateGroups = 0;
        for (Map.Entry<String, List<String>> e : byTuple.entrySet()) {
            if (e.getValue().size() <= 1) continue;
            duplicateGroups++;
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Duplicate aura effect tuple {}: shared by {}",
                    e.getKey(), e.getValue());
        }
        if (duplicateGroups == 0) {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "Aura uniqueness audit: {} presets, no duplicate effect tuples", PRESETS.size());
        }
    }

    private static String tupleKey(AuraPreset preset) {
        // Sorted pairs of (effect_id, amplifier) — order-insensitive.
        List<String> parts = new ArrayList<>(preset.effects().size());
        for (AuraPreset.EffectEntry entry : preset.effects()) {
            var effId = ForgeRegistries.MOB_EFFECTS.getKey(entry.effect());
            parts.add((effId != null ? effId.toString() : "unknown") + "@" + entry.amplifier());
        }
        Collections.sort(parts);
        return (preset.isOffensive() ? "off" : "pos") + ":" + String.join(",", parts);
    }

    private AuraRegistry() {}
}

package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.util.ConfigIO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Immersive Melodies instrument item IDs to mobile-tier aura preset IDs.
 * Mirrors {@link InstrumentAuraMapping}.
 *
 * <p>Added in 1.3.0 as a string-only schema ({@code "instrument": "aura_id"}).
 * 1.4.0 extends the schema to also accept object form
 * ({@code "instrument": {"default": "aura", "allowed": [...]}}) — same as the
 * stationary mapping — so admins can flip an instrument to its offensive
 * variant by editing {@code default}. Both forms are accepted; the string form
 * is migrated to object form on first boot of 1.4.0.
 */
public final class MobileInstrumentAuraMapping {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, MobileAuraConfig> MAPPINGS = new HashMap<>();

    /**
     * Pairing from positive mobile aura IDs to their 1.4.0 offensive counterpart.
     * Used by {@link #ensureOffensiveAllowedLists()}.
     */
    private static final Map<String, String> POSITIVE_TO_OFFENSIVE_MOBILE = Map.ofEntries(
            Map.entry("windstep_mobile", "gale_shock_mobile"),
            Map.entry("traveler_hum_mobile", "calamitys_chant_mobile"),
            Map.entry("measured_tempo_mobile", "iron_hammer_mobile"),
            Map.entry("hearthsong_mobile", "winters_chill_mobile"),
            Map.entry("earthpulse_mobile", "stone_ward_mobile"),
            Map.entry("steadfast_drone_mobile", "battle_march_mobile"),
            Map.entry("brass_call_mobile", "war_cry_mobile"),
            Map.entry("march_tap_mobile", "war_drum_mobile"),
            Map.entry("clear_ping_mobile", "echoing_chill_mobile"),
            Map.entry("stillwater_mobile", "abyssal_rattle_mobile"),
            Map.entry("shade_resonance_mobile", "soul_tremor_mobile")
    );

    /**
     * Mobile-tier aura config for a single instrument. Unlike the stationary
     * mapping this is consumed only by {@link #resolve(ResourceLocation)}, which
     * returns the {@code defaultAuraId} — there is no runtime selector in 1.4.0.
     * {@code allowedAuraIds} is carried so future versions (and tooling) can show
     * the allow-list without another schema migration.
     */
    public record MobileAuraConfig(String defaultAuraId, List<String> allowedAuraIds) {}

    private static Path getMappingFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/mobile_instrument_auras.json");
    }

    private static Path getMarkerFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.mobile_instrument_defaults_generated");
    }

    private static Path getOffensiveMigrationMarker() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.mobile_instrument_offensive_migration_done");
    }

    // --- Default generation ---

    public static void ensureDefaults() {
        ensureFirstRunDefaults();
        ensureOffensiveAllowedLists();
    }

    private static void ensureFirstRunDefaults() {
        Path marker = getMarkerFile();
        if (Files.exists(marker)) return;

        Path mappingFile = getMappingFile();
        try {
            Files.createDirectories(mappingFile.getParent());

            JsonObject root = new JsonObject();
            root.addProperty(AuraSchema.FIELD, AuraSchema.CURRENT_VERSION);
            root.addProperty("_comment",
                    "Maps Immersive Melodies instrument item IDs to mobile-tier passive aura configs. "
                            + "String value = single aura (default + only allowed). "
                            + "Object value = {\"default\": \"aura_id\", \"allowed\": [\"positive\", \"offensive\"]}. "
                            + "In 1.4.0 only 'default' is honored at runtime; 'allowed' is reserved for future UI. "
                            + "Reload with /effectiveinstruments reload. "
                            + "No-ops when Immersive Melodies is not installed.");

            addMobileEntry(root, "immersive_melodies:flute", "windstep_mobile", "gale_shock_mobile");
            addMobileEntry(root, "immersive_melodies:lute", "traveler_hum_mobile", "calamitys_chant_mobile");
            addMobileEntry(root, "immersive_melodies:piano", "measured_tempo_mobile", "iron_hammer_mobile");
            addMobileEntry(root, "immersive_melodies:vielle", "hearthsong_mobile", "winters_chill_mobile");
            addMobileEntry(root, "immersive_melodies:didgeridoo", "earthpulse_mobile", "stone_ward_mobile");
            addMobileEntry(root, "immersive_melodies:bagpipe", "steadfast_drone_mobile", "battle_march_mobile");
            addMobileEntry(root, "immersive_melodies:trumpet", "brass_call_mobile", "war_cry_mobile");
            addMobileEntry(root, "immersive_melodies:tiny_drum", "march_tap_mobile", "war_drum_mobile");
            addMobileEntry(root, "immersive_melodies:triangle", "clear_ping_mobile", "echoing_chill_mobile");
            addMobileEntry(root, "immersive_melodies:handpan", "stillwater_mobile", "abyssal_rattle_mobile");
            addMobileEntry(root, "immersive_melodies:ender_bass", "shade_resonance_mobile", "soul_tremor_mobile");

            ConfigIO.writeAtomically(mappingFile, GSON.toJson(root));
            Files.createFile(marker);
            Path offensiveMarker = getOffensiveMigrationMarker();
            if (!Files.exists(offensiveMarker)) {
                Files.createFile(offensiveMarker);
            }

            EffectiveInstrumentsMod.LOGGER.info("Generated default mobile instrument-aura mappings");
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to generate default mobile instrument-aura mappings", e);
        }
    }

    private static void addMobileEntry(JsonObject root, String instrumentId, String positive, String offensive) {
        JsonObject entry = new JsonObject();
        entry.addProperty("default", positive);
        JsonArray allowed = new JsonArray();
        allowed.add(positive);
        allowed.add(offensive);
        entry.add("allowed", allowed);
        root.add(instrumentId, entry);
    }

    /**
     * One-shot 1.4.0 migration: upgrades string-form mobile entries to object form
     * so the offensive alternative is documented alongside the positive default.
     * Only runs if the marker file is absent.
     */
    private static void ensureOffensiveAllowedLists() {
        Path marker = getOffensiveMigrationMarker();
        if (Files.exists(marker)) return;

        Path mappingFile = getMappingFile();
        if (!Files.exists(mappingFile)) return;

        String content;
        try {
            content = Files.readString(mappingFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error(
                    "Failed to read mobile_instrument_auras.json for offensive-migration, skipping", e);
            return;
        }

        JsonObject root;
        try {
            root = GSON.fromJson(content, JsonObject.class);
        } catch (JsonSyntaxException e) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Invalid JSON in mobile_instrument_auras.json; skipping offensive-migration: {}", e.getMessage());
            return;
        }
        if (root == null) return;

        int upgraded = 0;
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            keys.add(e.getKey());
        }

        for (String key : keys) {
            if (key.startsWith("_") || AuraSchema.FIELD.equals(key)) continue;
            JsonElement value = root.get(key);
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                continue;
            }
            String positiveId = value.getAsString();
            String offensiveId = POSITIVE_TO_OFFENSIVE_MOBILE.get(positiveId);
            if (offensiveId == null) continue;

            JsonObject upgradedEntry = new JsonObject();
            upgradedEntry.addProperty("default", positiveId);
            JsonArray allowed = new JsonArray();
            allowed.add(positiveId);
            allowed.add(offensiveId);
            upgradedEntry.add("allowed", allowed);
            root.add(key, upgradedEntry);
            upgraded++;
        }

        try {
            ConfigIO.writeAtomically(mappingFile, GSON.toJson(root));
            Files.createFile(marker);
            if (upgraded > 0) {
                EffectiveInstrumentsMod.LOGGER.info(
                        "Migrated {} mobile mapping entries to include offensive auras", upgraded);
            }
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error(
                    "Failed to write migrated mobile_instrument_auras.json (offensive pass)", e);
        }
    }

    // --- Loading ---

    public static void load() {
        MAPPINGS.clear();

        Path mappingFile = getMappingFile();
        if (!Files.exists(mappingFile)) {
            EffectiveInstrumentsMod.LOGGER.debug("No mobile instrument-aura mapping file found");
            return;
        }

        String content;
        try {
            content = Files.readString(mappingFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to read mobile instrument-aura mapping file", e);
            return;
        }

        JsonObject root;
        try {
            root = GSON.fromJson(content, JsonObject.class);
        } catch (JsonSyntaxException e) {
            EffectiveInstrumentsMod.LOGGER.error("Invalid JSON in mobile instrument-aura mapping: {}", e.getMessage());
            return;
        }
        if (root == null) return;

        // schemaVersion gate — same semantics as the stationary mapping.
        if (root.has(AuraSchema.FIELD)) {
            int version;
            try {
                version = root.get(AuraSchema.FIELD).getAsInt();
            } catch (Exception e) {
                EffectiveInstrumentsMod.LOGGER.error(
                        "mobile_instrument_auras.json has non-integer schemaVersion, ignoring file");
                return;
            }
            if (version > AuraSchema.CURRENT_VERSION) {
                EffectiveInstrumentsMod.LOGGER.error(
                        "mobile_instrument_auras.json has schemaVersion {} which is newer than this mod supports ({}), ignoring file",
                        version, AuraSchema.CURRENT_VERSION);
                return;
            }
        }

        int loaded = 0;
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) continue;
            if (AuraSchema.FIELD.equals(key)) continue;

            if (!ResourceLocation.isValidResourceLocation(key)) {
                EffectiveInstrumentsMod.LOGGER.warn("Invalid instrument ID '{}' in mobile mapping, skipping", key);
                continue;
            }

            MobileAuraConfig config = parseConfigEntry(key, entry.getValue());
            if (config == null) continue;

            MAPPINGS.put(new ResourceLocation(key), config);
            loaded++;

            // Warn early if the default aura doesn't resolve cleanly.
            java.util.Optional<AuraPreset> preset = AuraRegistry.getById(config.defaultAuraId());
            if (preset.isEmpty()) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Mobile mapping '{}' default aura '{}' is unknown — it will have no effect", key, config.defaultAuraId());
            } else if (!preset.get().supports(BuffTier.MOBILE)) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Mobile mapping '{}' default aura '{}' does not support the mobile tier", key, config.defaultAuraId());
            }
        }

        // Self-healing: augment each config's allowed list with the offensive
        // counterpart if missing. Same approach as InstrumentAuraMapping —
        // in-memory primary, opportunistic rewrite secondary.
        int synthesized = synthesizeMissingOffensiveAllowed();
        if (synthesized > 0) {
            EffectiveInstrumentsMod.LOGGER.info(
                    "Auto-added offensive aura to {} mobile mapping(s). Opportunistically rewriting file.", synthesized);
            tryRewriteMappingFile();
        }

        EffectiveInstrumentsMod.LOGGER.info("Loaded {} mobile instrument-aura mappings", loaded);
    }

    private static int synthesizeMissingOffensiveAllowed() {
        int count = 0;
        for (Map.Entry<ResourceLocation, MobileAuraConfig> e : MAPPINGS.entrySet()) {
            MobileAuraConfig config = e.getValue();
            String offensiveId = POSITIVE_TO_OFFENSIVE_MOBILE.get(config.defaultAuraId());
            if (offensiveId == null) continue;
            if (config.allowedAuraIds().contains(offensiveId)) continue;

            var preset = AuraRegistry.getById(offensiveId);
            if (preset.isEmpty() || !preset.get().enabled() || !preset.get().supports(BuffTier.MOBILE)) {
                continue;
            }

            List<String> merged = new ArrayList<>(config.allowedAuraIds());
            merged.add(offensiveId);
            e.setValue(new MobileAuraConfig(config.defaultAuraId(), List.copyOf(merged)));
            count++;
        }
        return count;
    }

    private static void tryRewriteMappingFile() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty(AuraSchema.FIELD, AuraSchema.CURRENT_VERSION);
            root.addProperty("_comment",
                    "Auto-rewritten by 1.4.1 self-healing pass. Admins can still edit freely — "
                            + "each entry is {\"default\": \"aura\", \"allowed\": [...]}. "
                            + "Reload with /effectiveinstruments reload.");
            for (Map.Entry<ResourceLocation, MobileAuraConfig> e : MAPPINGS.entrySet()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("default", e.getValue().defaultAuraId());
                JsonArray allowed = new JsonArray();
                for (String id : e.getValue().allowedAuraIds()) allowed.add(id);
                entry.add("allowed", allowed);
                root.add(e.getKey().toString(), entry);
            }
            ConfigIO.writeAtomically(getMappingFile(), GSON.toJson(root));
            Path marker = getOffensiveMigrationMarker();
            if (!Files.exists(marker)) {
                try {
                    Files.createFile(marker);
                } catch (IOException ignored) {
                }
            }
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Could not rewrite mobile_instrument_auras.json after offensive synthesis: {}", e.getMessage());
        }
    }

    @Nullable
    private static MobileAuraConfig parseConfigEntry(String instrumentKey, JsonElement value) {
        if (value == null || value.isJsonNull()) return null;

        // String shorthand (1.3.x form): "instrument": "aura_id"
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String auraId = value.getAsString();
            if (auraId.isEmpty()) return null;
            return new MobileAuraConfig(auraId, List.of(auraId));
        }

        // Object form (1.4.0+): {"default": "aura", "allowed": ["positive", "offensive"]}
        if (value.isJsonObject()) {
            JsonObject obj = value.getAsJsonObject();
            if (!obj.has("default") || !obj.get("default").isJsonPrimitive()) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Mobile mapping '{}' missing 'default' field, skipping", instrumentKey);
                return null;
            }
            String defaultAuraId = obj.get("default").getAsString();
            if (defaultAuraId.isEmpty()) return null;

            Set<String> allowed = new LinkedHashSet<>();
            allowed.add(defaultAuraId);
            if (obj.has("allowed") && obj.get("allowed").isJsonArray()) {
                for (JsonElement elem : obj.getAsJsonArray("allowed")) {
                    if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isString()) continue;
                    String id = elem.getAsString();
                    if (!id.isEmpty()) allowed.add(id);
                }
            }
            return new MobileAuraConfig(defaultAuraId, List.copyOf(allowed));
        }

        EffectiveInstrumentsMod.LOGGER.warn(
                "Mobile mapping '{}' value is neither a string nor an object, skipping", instrumentKey);
        return null;
    }

    // --- Query ---

    /** True if {@code instrumentId} is present in the mobile mapping (regardless of aura validity). */
    public static boolean hasMapping(ResourceLocation instrumentId) {
        return MAPPINGS.containsKey(instrumentId);
    }

    /**
     * Resolve the mobile aura for an instrument. Returns {@code null} if the
     * instrument is unmapped, the aura id is unknown, the preset is disabled,
     * or the preset does not support the mobile tier. Uses the {@code default}
     * entry from the mapping; {@code allowed} is reserved for future UI.
     */
    @Nullable
    public static AuraPreset resolve(ResourceLocation instrumentId) {
        MobileAuraConfig config = MAPPINGS.get(instrumentId);
        if (config == null) return null;
        return AuraRegistry.getById(config.defaultAuraId())
                .filter(AuraPreset::enabled)
                .filter(p -> p.supports(BuffTier.MOBILE))
                .orElse(null);
    }

    /**
     * Resolve the mobile aura for a specific player + instrument. Prefers the
     * player's stored selection from {@link MobilePlayerSelection}; falls back
     * to the default if the selection is absent, disabled, or not in the
     * allow-list. Returns {@code null} for unmapped instruments or when the
     * resolved preset doesn't support the mobile tier.
     *
     * @param server   live server — provided because the SavedData lives on the overworld.
     *                 Pass {@code null} to skip the per-player lookup (behaves like {@link #resolve}).
     */
    @Nullable
    public static AuraPreset resolveFor(
            @Nullable net.minecraft.server.MinecraftServer server,
            java.util.UUID playerId,
            ResourceLocation instrumentId
    ) {
        MobileAuraConfig config = MAPPINGS.get(instrumentId);
        if (config == null) return null;

        String chosenId = null;
        if (server != null) {
            String selected = MobilePlayerSelection.get(server).getSelection(playerId, instrumentId);
            if (selected != null && config.allowedAuraIds().contains(selected)) {
                chosenId = selected;
            }
        }
        if (chosenId == null) chosenId = config.defaultAuraId();

        return AuraRegistry.getById(chosenId)
                .filter(AuraPreset::enabled)
                .filter(p -> p.supports(BuffTier.MOBILE))
                .orElse(null);
    }

    @Nullable
    public static MobileAuraConfig getConfig(ResourceLocation instrumentId) {
        return MAPPINGS.get(instrumentId);
    }

    /**
     * Return the list of selectable mobile auras for an IM instrument, preserving
     * allowed-list ordering. Unlike the stationary sibling, this does NOT filter
     * by {@code showInSelector} — mobile offensive presets intentionally ship
     * with {@code showInSelector=false} so they stay out of the stationary
     * overlay, but we still want them in the IM-screen overlay and the keybind
     * picker. Filters for {@code enabled} + mobile tier support only.
     */
    public static java.util.List<AuraPreset> getAllowedAuras(@Nullable ResourceLocation instrumentId) {
        if (instrumentId == null) return java.util.List.of();
        MobileAuraConfig config = MAPPINGS.get(instrumentId);
        if (config == null) return java.util.List.of();
        java.util.List<AuraPreset> result = new java.util.ArrayList<>();
        for (String auraId : config.allowedAuraIds()) {
            AuraRegistry.getById(auraId)
                    .filter(AuraPreset::enabled)
                    .filter(p -> p.supports(BuffTier.MOBILE))
                    .ifPresent(result::add);
        }
        return result;
    }

    public static int getMappingCount() {
        return MAPPINGS.size();
    }

    private MobileInstrumentAuraMapping() {}
}

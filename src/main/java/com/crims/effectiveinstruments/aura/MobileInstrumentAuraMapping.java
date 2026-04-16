package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Maps Immersive Melodies instrument item IDs to mobile-tier aura preset IDs.
 * Mirrors {@link InstrumentAuraMapping} but uses a distinct config file, a
 * distinct marker, and a simpler string-only schema (each entry is exactly
 * one aura — no allow-list, no selector).
 *
 * <p>Added in 1.3.0. Independent marker {@code .mobile_instrument_defaults_generated}
 * ensures upgraded installs receive these defaults on their next boot.
 */
public final class MobileInstrumentAuraMapping {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, String> MAPPINGS = new HashMap<>();

    private static Path getMappingFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/mobile_instrument_auras.json");
    }

    private static Path getMarkerFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.mobile_instrument_defaults_generated");
    }

    // --- Default generation ---

    public static void ensureDefaults() {
        Path marker = getMarkerFile();
        if (Files.exists(marker)) return;

        Path mappingFile = getMappingFile();
        try {
            Files.createDirectories(mappingFile.getParent());

            JsonObject root = new JsonObject();
            root.addProperty(AuraSchema.FIELD, AuraSchema.CURRENT_VERSION);
            root.addProperty("_comment",
                    "Maps Immersive Melodies instrument item IDs to mobile-tier passive aura IDs. "
                            + "One aura per instrument (no allow-list). "
                            + "Reload with /effectiveinstruments reload. "
                            + "No-ops when Immersive Melodies is not installed.");

            root.addProperty("immersive_melodies:flute", "windstep_mobile");
            root.addProperty("immersive_melodies:lute", "traveler_hum_mobile");
            root.addProperty("immersive_melodies:piano", "measured_tempo_mobile");
            root.addProperty("immersive_melodies:vielle", "hearthsong_mobile");
            root.addProperty("immersive_melodies:didgeridoo", "earthpulse_mobile");
            root.addProperty("immersive_melodies:bagpipe", "steadfast_drone_mobile");
            root.addProperty("immersive_melodies:trumpet", "brass_call_mobile");
            root.addProperty("immersive_melodies:tiny_drum", "march_tap_mobile");
            root.addProperty("immersive_melodies:triangle", "clear_ping_mobile");
            root.addProperty("immersive_melodies:handpan", "stillwater_mobile");
            root.addProperty("immersive_melodies:ender_bass", "shade_resonance_mobile");

            Files.writeString(mappingFile, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.createFile(marker);

            EffectiveInstrumentsMod.LOGGER.info("Generated default mobile instrument-aura mappings");
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to generate default mobile instrument-aura mappings", e);
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

            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Mobile mapping entry '{}' is not a string (expected aura id), skipping", key);
                continue;
            }

            String auraId = value.getAsString();
            MAPPINGS.put(new ResourceLocation(key), auraId);
            loaded++;

            // Warn early if the referenced aura doesn't exist or won't resolve at runtime.
            java.util.Optional<AuraPreset> preset = AuraRegistry.getById(auraId);
            if (preset.isEmpty()) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Mobile mapping '{}' references unknown aura '{}' — it will have no effect", key, auraId);
            } else if (!preset.get().supports(BuffTier.MOBILE)) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Mobile mapping '{}' references aura '{}' which does not support the mobile tier", key, auraId);
            }
        }

        EffectiveInstrumentsMod.LOGGER.info("Loaded {} mobile instrument-aura mappings", loaded);
    }

    // --- Query ---

    /** True if {@code instrumentId} is present in the mobile mapping (regardless of aura validity). */
    public static boolean hasMapping(ResourceLocation instrumentId) {
        return MAPPINGS.containsKey(instrumentId);
    }

    /**
     * Resolve the mobile aura for an instrument. Returns {@code null} if the
     * instrument is unmapped, the aura id is unknown, the preset is disabled,
     * or the preset does not support the mobile tier.
     */
    @Nullable
    public static AuraPreset resolve(ResourceLocation instrumentId) {
        String auraId = MAPPINGS.get(instrumentId);
        if (auraId == null) return null;
        return AuraRegistry.getById(auraId)
                .filter(AuraPreset::enabled)
                .filter(p -> p.supports(BuffTier.MOBILE))
                .orElse(null);
    }

    public static int getMappingCount() {
        return MAPPINGS.size();
    }

    private MobileInstrumentAuraMapping() {}
}

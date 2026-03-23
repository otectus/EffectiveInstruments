package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class InstrumentAuraMapping {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, InstrumentAuraConfig> MAPPINGS = new HashMap<>();

    /**
     * Configuration for a single instrument's aura setup.
     * @param defaultAuraId the aura auto-selected when the instrument opens
     * @param allowedAuraIds all auras shown in the selector (must include the default)
     */
    public record InstrumentAuraConfig(String defaultAuraId, List<String> allowedAuraIds) {}

    private static Path getMappingFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/instrument_auras.json");
    }

    private static Path getMarkerFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.instrument_defaults_generated");
    }

    // --- Default generation ---

    public static void ensureDefaults() {
        Path marker = getMarkerFile();
        if (Files.exists(marker)) return;

        Path mappingFile = getMappingFile();
        try {
            Files.createDirectories(mappingFile.getParent());

            JsonObject root = new JsonObject();
            root.addProperty("_comment",
                    "Maps instrument IDs to aura configs. "
                            + "String value = single aura (default + only allowed). "
                            + "Object value = {\"default\": \"aura_id\", \"allowed\": [\"aura1\", \"aura2\"]}. "
                            + "Remove an entry to show all auras for that instrument. "
                            + "Reload with /effectiveinstruments reload.");
            // Genshin Instruments
            root.addProperty("genshinstrument:windsong_lyre", "zephyrs_blessing");
            root.addProperty("genshinstrument:vintage_lyre", "echoes_of_antiquity");
            root.addProperty("genshinstrument:floral_zither", "bloom_veil");
            root.addProperty("genshinstrument:glorious_drum", "warcry_cadence");
            root.addProperty("genshinstrument:nightwind_horn", "moonlit_passage");
            root.addProperty("genshinstrument:ukulele", "sunkissed_serenade");
            root.addProperty("genshinstrument:djem_djem_drum", "rhythm_of_the_earth");

            // Even More Instruments
            root.addProperty("evenmoreinstruments:guitar", "wanderers_anthem");
            root.addProperty("evenmoreinstruments:keyboard", "harmonic_resonance");
            root.addProperty("evenmoreinstruments:koto", "tranquil_current");
            root.addProperty("evenmoreinstruments:pipa", "silk_road_vigor");
            root.addProperty("evenmoreinstruments:saxophone", "smoky_allure");
            root.addProperty("evenmoreinstruments:shamisen", "ghost_flame");
            root.addProperty("evenmoreinstruments:trombone", "bulwark_fanfare");
            root.addProperty("evenmoreinstruments:violin", "heartstring_aria");

            // Even More Instruments — Note Block Instrument variants
            root.addProperty("evenmoreinstruments:harp_note_block_instrument", "zephyrs_blessing");
            root.addProperty("evenmoreinstruments:bass_note_block_instrument", "wanderers_anthem");
            root.addProperty("evenmoreinstruments:basedrum_note_block_instrument", "warcry_cadence");
            root.addProperty("evenmoreinstruments:snare_note_block_instrument", "rhythm_of_the_earth");
            root.addProperty("evenmoreinstruments:hat_note_block_instrument", "rhythm_of_the_earth");
            root.addProperty("evenmoreinstruments:bell_note_block_instrument", "sunkissed_serenade");
            root.addProperty("evenmoreinstruments:chime_note_block_instrument", "moonlit_passage");
            root.addProperty("evenmoreinstruments:flute_note_block_instrument", "zephyrs_blessing");
            root.addProperty("evenmoreinstruments:guitar_note_block_instrument", "wanderers_anthem");
            root.addProperty("evenmoreinstruments:xylophone_note_block_instrument", "harmonic_resonance");
            root.addProperty("evenmoreinstruments:iron_xylophone_note_block_instrument", "harmonic_resonance");
            root.addProperty("evenmoreinstruments:cow_bell_note_block_instrument", "sunkissed_serenade");
            root.addProperty("evenmoreinstruments:didgeridoo_note_block_instrument", "moonlit_passage");
            root.addProperty("evenmoreinstruments:bit_note_block_instrument", "harmonic_resonance");
            root.addProperty("evenmoreinstruments:banjo_note_block_instrument", "sunkissed_serenade");
            root.addProperty("evenmoreinstruments:pling_note_block_instrument", "harmonic_resonance");

            Files.writeString(mappingFile, GSON.toJson(root), StandardCharsets.UTF_8);
            writeReadme(mappingFile.getParent());
            Files.createFile(marker);

            EffectiveInstrumentsMod.LOGGER.info("Generated default instrument-aura mappings");
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to generate default instrument-aura mappings", e);
        }
    }

    private static void writeReadme(Path dir) throws IOException {
        String readme = """
                === Effective Instruments — Instrument-Aura Mapping ===

                The file "instrument_auras.json" maps instrument IDs to aura configs.
                When a player opens an instrument, only the allowed auras are shown
                in the selector, and the default aura is auto-selected.

                --- Format ---

                Two formats are supported per entry:

                1. String shorthand (single aura — default and only option):
                   "genshinstrument:windsong_lyre": "zephyrs_blessing"

                2. Object form (default + additional allowed auras):
                   "genshinstrument:windsong_lyre": {
                     "default": "zephyrs_blessing",
                     "allowed": ["zephyrs_blessing", "echoes_of_antiquity"]
                   }

                If "allowed" is omitted in object form, it defaults to just the default aura.
                The default aura is always included in the allowed list automatically.

                To show ALL auras for an instrument, remove its entry entirely.
                Unknown instrument IDs are silently ignored.
                Invalid aura IDs are logged as warnings and skipped.

                --- Finding Instrument IDs ---

                Genshin Instruments:
                  genshinstrument:windsong_lyre
                  genshinstrument:vintage_lyre
                  genshinstrument:floral_zither
                  genshinstrument:glorious_drum
                  genshinstrument:nightwind_horn
                  genshinstrument:ukulele
                  genshinstrument:djem_djem_drum

                Even More Instruments (if installed):
                  evenmoreinstruments:guitar
                  evenmoreinstruments:keyboard
                  evenmoreinstruments:koto
                  evenmoreinstruments:pipa
                  evenmoreinstruments:saxophone
                  evenmoreinstruments:shamisen
                  evenmoreinstruments:trombone
                  evenmoreinstruments:violin

                Even More Instruments — Note Block variants:
                  evenmoreinstruments:harp_note_block_instrument
                  evenmoreinstruments:bass_note_block_instrument
                  evenmoreinstruments:basedrum_note_block_instrument
                  evenmoreinstruments:snare_note_block_instrument
                  evenmoreinstruments:hat_note_block_instrument
                  evenmoreinstruments:bell_note_block_instrument
                  evenmoreinstruments:chime_note_block_instrument
                  evenmoreinstruments:flute_note_block_instrument
                  evenmoreinstruments:guitar_note_block_instrument
                  evenmoreinstruments:xylophone_note_block_instrument
                  evenmoreinstruments:iron_xylophone_note_block_instrument
                  evenmoreinstruments:cow_bell_note_block_instrument
                  evenmoreinstruments:didgeridoo_note_block_instrument
                  evenmoreinstruments:bit_note_block_instrument
                  evenmoreinstruments:banjo_note_block_instrument
                  evenmoreinstruments:pling_note_block_instrument

                Use /effectiveinstruments reload to apply changes without restarting.
                """;
        Files.writeString(dir.resolve("_README_INSTRUMENTS.txt"), readme, StandardCharsets.UTF_8);
    }

    // --- Loading ---

    public static void load() {
        MAPPINGS.clear();

        Path mappingFile = getMappingFile();
        if (!Files.exists(mappingFile)) {
            EffectiveInstrumentsMod.LOGGER.debug("No instrument-aura mapping file found");
            return;
        }

        String content;
        try {
            content = Files.readString(mappingFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to read instrument-aura mapping file", e);
            return;
        }

        JsonObject root;
        try {
            root = GSON.fromJson(content, JsonObject.class);
        } catch (JsonSyntaxException e) {
            EffectiveInstrumentsMod.LOGGER.error("Invalid JSON in instrument-aura mapping file: {}", e.getMessage());
            return;
        }

        if (root == null) return;

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) continue;

            if (!ResourceLocation.isValidResourceLocation(key)) {
                EffectiveInstrumentsMod.LOGGER.warn("Invalid instrument ID '{}' in mapping, skipping", key);
                continue;
            }

            JsonElement value = entry.getValue();
            InstrumentAuraConfig config = parseConfigEntry(key, value);
            if (config != null) {
                MAPPINGS.put(new ResourceLocation(key), config);
            }
        }

        EffectiveInstrumentsMod.LOGGER.info("Loaded {} instrument-aura mappings", MAPPINGS.size());
    }

    @Nullable
    private static InstrumentAuraConfig parseConfigEntry(String instrumentKey, JsonElement value) {
        if (value.isJsonNull()) return null;

        // String shorthand: "instrument": "aura_id"
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String auraId = value.getAsString();
            if (auraId.isEmpty()) return null;
            if (AuraRegistry.getById(auraId).isEmpty()) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Instrument '{}' mapped to unknown aura '{}', skipping", instrumentKey, auraId);
                return null;
            }
            return new InstrumentAuraConfig(auraId, List.of(auraId));
        }

        // Object form: {"default": "aura_id", "allowed": ["aura1", "aura2"]}
        if (value.isJsonObject()) {
            JsonObject obj = value.getAsJsonObject();

            if (!obj.has("default") || !obj.get("default").isJsonPrimitive()) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Instrument '{}' config missing 'default' field, skipping", instrumentKey);
                return null;
            }

            String defaultAuraId = obj.get("default").getAsString();
            if (defaultAuraId.isEmpty() || AuraRegistry.getById(defaultAuraId).isEmpty()) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Instrument '{}' has invalid default aura '{}', skipping", instrumentKey, defaultAuraId);
                return null;
            }

            // Parse allowed list
            Set<String> allowedSet = new LinkedHashSet<>();
            allowedSet.add(defaultAuraId); // default is always allowed

            if (obj.has("allowed") && obj.get("allowed").isJsonArray()) {
                for (JsonElement elem : obj.getAsJsonArray("allowed")) {
                    if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isString()) continue;
                    String auraId = elem.getAsString();
                    if (auraId.isEmpty()) continue;
                    if (AuraRegistry.getById(auraId).isEmpty()) {
                        EffectiveInstrumentsMod.LOGGER.warn(
                                "Instrument '{}' lists unknown allowed aura '{}', skipping", instrumentKey, auraId);
                        continue;
                    }
                    allowedSet.add(auraId);
                }
            }

            return new InstrumentAuraConfig(defaultAuraId, List.copyOf(allowedSet));
        }

        EffectiveInstrumentsMod.LOGGER.warn(
                "Invalid value type for instrument '{}', expected string or object", instrumentKey);
        return null;
    }

    // --- Queries ---

    @Nullable
    public static String getDefaultAuraId(ResourceLocation instrumentId) {
        InstrumentAuraConfig config = MAPPINGS.get(instrumentId);
        return config != null ? config.defaultAuraId() : null;
    }

    @Nullable
    public static InstrumentAuraConfig getConfig(ResourceLocation instrumentId) {
        return MAPPINGS.get(instrumentId);
    }

    /**
     * Returns the list of allowed auras for an instrument.
     * If the instrument has a mapping, returns only the allowed auras (resolved from registry).
     * If unmapped or null, returns all enabled auras (backwards-compatible).
     */
    public static List<AuraPreset> getAllowedAuras(@Nullable ResourceLocation instrumentId) {
        if (instrumentId == null) {
            return AuraRegistry.getEnabledPresets();
        }

        InstrumentAuraConfig config = MAPPINGS.get(instrumentId);
        if (config == null) {
            return AuraRegistry.getEnabledPresets();
        }

        List<AuraPreset> result = new ArrayList<>();
        for (String auraId : config.allowedAuraIds()) {
            AuraRegistry.getById(auraId)
                    .filter(AuraPreset::enabled)
                    .ifPresent(result::add);
        }
        return result;
    }

    public static int getMappingCount() {
        return MAPPINGS.size();
    }

    private InstrumentAuraMapping() {}
}

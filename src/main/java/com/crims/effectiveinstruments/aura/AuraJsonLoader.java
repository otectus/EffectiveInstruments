package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.google.gson.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class AuraJsonLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path getAurasDir() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/auras");
    }

    private static Path getMarkerFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.defaults_generated");
    }

    /**
     * Distinct marker for the mobile-tier preset defaults added in 1.3.0. Using a
     * separate marker from {@link #getMarkerFile()} is the non-negotiable migration
     * detail — upgraded installs already have the stationary marker, so reusing it
     * would silently skip the new mobile defaults.
     */
    private static Path getMobileMarkerFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.mobile_aura_defaults_generated");
    }

    // --- Default generation ---

    public static void ensureDefaults() {
        ensureStationaryDefaults();
        ensureMobileDefaults();
    }

    private static void ensureStationaryDefaults() {
        Path marker = getMarkerFile();
        if (Files.exists(marker)) return;

        Path aurasDir = getAurasDir();
        try {
            Files.createDirectories(aurasDir);

            // --- Genshin Instruments auras ---

            writeDefaultJson(aurasDir, "zephyrs_blessing", "Zephyr's Blessing",
                    "A gentle breeze quickens the step of nearby allies", "7FDBCA", 160, 0,
                    new String[][]{{"minecraft:speed", "0"}},
                    "effectiveinstruments:textures/gui/aura_zephyrs_blessing.png",
                    "effectiveinstruments:textures/gui/aura_zephyrs_blessing_selected.png");

            writeDefaultJson(aurasDir, "echoes_of_antiquity", "Echoes of Antiquity",
                    "Ancient melodies mend the wounds of nearby allies", "D4A574", 160, 1,
                    new String[][]{{"minecraft:regeneration", "0"}},
                    "effectiveinstruments:textures/gui/aura_echoes_of_antiquity.png",
                    "effectiveinstruments:textures/gui/aura_echoes_of_antiquity_selected.png");

            writeDefaultJson(aurasDir, "bloom_veil", "Bloom Veil",
                    "Nourishes and shields nearby allies with floral energy", "FF88CC", 160, 2,
                    new String[][]{{"minecraft:absorption", "0"}, {"minecraft:saturation", "0"}},
                    "effectiveinstruments:textures/gui/aura_bloom_veil.png",
                    "effectiveinstruments:textures/gui/aura_bloom_veil_selected.png");

            writeDefaultJson(aurasDir, "warcry_cadence", "Warcry Cadence",
                    "Emboldens nearby allies with strength and resilience", "FF4422", 160, 3,
                    new String[][]{{"minecraft:strength", "0"}, {"minecraft:resistance", "0"}},
                    "effectiveinstruments:textures/gui/aura_warcry_cadence.png",
                    "effectiveinstruments:textures/gui/aura_warcry_cadence_selected.png");

            writeDefaultJson(aurasDir, "moonlit_passage", "Moonlit Passage",
                    "Grants night sight and safe descent to nearby allies", "6644BB", 260, 4,
                    new String[][]{{"minecraft:night_vision", "0"}, {"minecraft:slow_falling", "0"}},
                    "effectiveinstruments:textures/gui/aura_moonlit_passage.png",
                    "effectiveinstruments:textures/gui/aura_moonlit_passage_selected.png");

            writeDefaultJson(aurasDir, "sunkissed_serenade", "Sunkissed Serenade",
                    "Fortune favors nearby allies with improved luck", "FFDD44", 200, 5,
                    new String[][]{{"minecraft:luck", "0"}},
                    "effectiveinstruments:textures/gui/aura_sunkissed_serenade.png",
                    "effectiveinstruments:textures/gui/aura_sunkissed_serenade_selected.png");

            writeDefaultJson(aurasDir, "rhythm_of_the_earth", "Rhythm of the Earth",
                    "Primal beats hasten the hands and spring the step of nearby allies", "CC6633", 160, 6,
                    new String[][]{{"minecraft:haste", "0"}, {"minecraft:jump_boost", "0"}},
                    "effectiveinstruments:textures/gui/aura_rhythm_of_the_earth.png",
                    "effectiveinstruments:textures/gui/aura_rhythm_of_the_earth_selected.png");

            // --- Even More Instruments auras ---

            writeDefaultJson(aurasDir, "wanderers_anthem", "Wanderer's Anthem",
                    "Quickens stride and lightens step for nearby allies", "B8860B", 160, 7,
                    new String[][]{{"minecraft:speed", "0"}, {"minecraft:jump_boost", "0"}},
                    "effectiveinstruments:textures/gui/aura_wanderers_anthem.png",
                    "effectiveinstruments:textures/gui/aura_wanderers_anthem_selected.png");

            writeDefaultJson(aurasDir, "harmonic_resonance", "Harmonic Resonance",
                    "Precise harmonies restore and energize nearby allies", "EEEEFF", 160, 8,
                    new String[][]{{"minecraft:regeneration", "0"}, {"minecraft:haste", "0"}},
                    "effectiveinstruments:textures/gui/aura_harmonic_resonance.png",
                    "effectiveinstruments:textures/gui/aura_harmonic_resonance_selected.png");

            writeDefaultJson(aurasDir, "tranquil_current", "Tranquil Current",
                    "Flowing tones grant water breathing and aquatic grace to nearby allies", "44AACC", 260, 9,
                    new String[][]{{"minecraft:water_breathing", "0"}, {"minecraft:dolphins_grace", "0"}},
                    "effectiveinstruments:textures/gui/aura_tranquil_current.png",
                    "effectiveinstruments:textures/gui/aura_tranquil_current_selected.png");

            writeDefaultJson(aurasDir, "silk_road_vigor", "Silk Road Vigor",
                    "Fierce melodies drive nearby allies to move and strike with vigor", "FF6644", 160, 10,
                    new String[][]{{"minecraft:speed", "0"}, {"minecraft:strength", "0"}},
                    "effectiveinstruments:textures/gui/aura_silk_road_vigor.png",
                    "effectiveinstruments:textures/gui/aura_silk_road_vigor_selected.png");

            writeDefaultJson(aurasDir, "smoky_allure", "Smoky Allure",
                    "Charismatic jazz melodies charm villagers into offering better trades", "DAA520", 200, 11,
                    new String[][]{{"minecraft:hero_of_the_village", "0"}},
                    "effectiveinstruments:textures/gui/aura_smoky_allure.png",
                    "effectiveinstruments:textures/gui/aura_smoky_allure_selected.png");

            writeDefaultJson(aurasDir, "ghost_flame", "Ghost Flame",
                    "Spectral fire shields from flame and emboldens nearby allies", "88CCFF", 200, 12,
                    new String[][]{{"minecraft:fire_resistance", "0"}, {"minecraft:strength", "0"}},
                    "effectiveinstruments:textures/gui/aura_ghost_flame.png",
                    "effectiveinstruments:textures/gui/aura_ghost_flame_selected.png");

            writeDefaultJson(aurasDir, "bulwark_fanfare", "Bulwark Fanfare",
                    "Triumphant brass fortifies and shields nearby allies", "CC8800", 160, 13,
                    new String[][]{{"minecraft:resistance", "0"}, {"minecraft:absorption", "1"}},
                    "effectiveinstruments:textures/gui/aura_bulwark_fanfare.png",
                    "effectiveinstruments:textures/gui/aura_bulwark_fanfare_selected.png");

            writeDefaultJson(aurasDir, "heartstring_aria", "Heartstring Aria",
                    "A soaring melody that heals and shields nearby allies", "CC4466", 200, 14,
                    new String[][]{{"minecraft:regeneration", "0"}, {"minecraft:absorption", "0"}},
                    "effectiveinstruments:textures/gui/aura_heartstring_aria.png",
                    "effectiveinstruments:textures/gui/aura_heartstring_aria_selected.png");

            writeReadme(aurasDir);
            Files.createFile(marker);

            EffectiveInstrumentsMod.LOGGER.info("Generated default aura presets in {}", aurasDir);
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to generate default aura presets", e);
        }
    }

    private static void writeDefaultJson(Path dir, String id, String displayName, String description,
                                          String color, int durationTicks, int sortOrder,
                                          String[][] effects) throws IOException {
        writeDefaultJson(dir, id, displayName, description, color, durationTicks, sortOrder, effects, null, null);
    }

    /**
     * Generate the mobile-tier preset JSONs (1.3.0+). Guarded by a distinct marker
     * from the stationary defaults so upgraded installs receive these on their
     * next boot even if the stationary marker already exists.
     */
    private static void ensureMobileDefaults() {
        Path marker = getMobileMarkerFile();
        if (Files.exists(marker)) return;

        Path aurasDir = getAurasDir();
        try {
            Files.createDirectories(aurasDir);

            // Each mobile preset: single effect, 60-tick duration, radius -1 → mobile default radius.
            // sortOrder starts at 200 to land below stationary presets (0-14) in any list view.
            writeMobileDefault(aurasDir, "windstep_mobile", "Windstep",
                    "A light melody that keeps nearby allies nimble while on the move", "7FDBCA", 60, 200,
                    "minecraft:speed", 0);
            writeMobileDefault(aurasDir, "traveler_hum_mobile", "Traveler's Hum",
                    "A wandering tune that stirs fortune for travellers nearby", "D4A574", 60, 201,
                    "minecraft:luck", 0);
            writeMobileDefault(aurasDir, "measured_tempo_mobile", "Measured Tempo",
                    "A steady rhythm that quickens the hands of nearby allies", "EEEEFF", 60, 202,
                    "minecraft:haste", 0);
            writeMobileDefault(aurasDir, "hearthsong_mobile", "Hearthsong",
                    "A warm hearth-song that slowly mends nearby allies", "CC4466", 60, 203,
                    "minecraft:regeneration", 0);
            writeMobileDefault(aurasDir, "earthpulse_mobile", "Earthpulse",
                    "A deep drone that lightens the step of nearby allies", "CC6633", 60, 204,
                    "minecraft:jump_boost", 0);
            writeMobileDefault(aurasDir, "steadfast_drone_mobile", "Steadfast Drone",
                    "A resolute drone that steels nearby allies against harm", "CC8800", 60, 205,
                    "minecraft:resistance", 0);
            writeMobileDefault(aurasDir, "brass_call_mobile", "Brass Call",
                    "A brassy call that emboldens nearby allies to strike harder", "FF6644", 40, 206,
                    "minecraft:strength", 0);
            writeMobileDefault(aurasDir, "march_tap_mobile", "March Tap",
                    "A marching beat that quickens nearby allies on the road", "B8860B", 60, 207,
                    "minecraft:speed", 0);
            writeMobileDefault(aurasDir, "clear_ping_mobile", "Clear Ping",
                    "A pure tone that sharpens night-sight for nearby allies", "6644BB", 100, 208,
                    "minecraft:night_vision", 0);
            writeMobileDefault(aurasDir, "stillwater_mobile", "Stillwater",
                    "A tranquil resonance that lets nearby allies breathe underwater", "44AACC", 100, 209,
                    "minecraft:water_breathing", 0);
            writeMobileDefault(aurasDir, "shade_resonance_mobile", "Shade Resonance",
                    "A deep, cool resonance that shields nearby allies from flame", "88CCFF", 80, 210,
                    "minecraft:fire_resistance", 0);

            Files.createFile(marker);
            EffectiveInstrumentsMod.LOGGER.info("Generated mobile-tier default aura presets");
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to generate mobile-tier default aura presets", e);
        }
    }

    /**
     * Helper for writing a single-effect, mobile-only preset. Emits
     * {@code "tiers": ["mobile"]} and {@code "showInSelector": false} explicitly
     * so the parser lands in the mobile branch and the selector UI never sees it.
     */
    private static void writeMobileDefault(
            Path dir, String id, String displayName, String description,
            String color, int durationTicks, int sortOrder,
            String effectId, int amplifier
    ) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty(AuraSchema.FIELD, AuraSchema.CURRENT_VERSION);
        root.addProperty("displayName", displayName);
        root.addProperty("description", description);
        root.addProperty("color", color);
        root.addProperty("enabled", true);
        root.addProperty("durationTicks", durationTicks);
        root.addProperty("radius", -1);
        root.addProperty("sortOrder", sortOrder);

        JsonArray tiers = new JsonArray();
        tiers.add("mobile");
        root.add("tiers", tiers);
        root.addProperty("showInSelector", false);

        JsonArray effectsArray = new JsonArray();
        JsonObject eff = new JsonObject();
        eff.addProperty("effect", effectId);
        eff.addProperty("amplifier", amplifier);
        effectsArray.add(eff);
        root.add("effects", effectsArray);

        Files.writeString(dir.resolve(id + ".json"), GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static void writeDefaultJson(Path dir, String id, String displayName, String description,
                                          String color, int durationTicks, int sortOrder,
                                          String[][] effects, String icon, String iconSelected) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty(AuraSchema.FIELD, AuraSchema.CURRENT_VERSION);
        root.addProperty("displayName", displayName);
        root.addProperty("description", description);
        root.addProperty("color", color);
        root.addProperty("enabled", true);
        root.addProperty("durationTicks", durationTicks);
        root.addProperty("radius", -1);
        root.addProperty("sortOrder", sortOrder);

        JsonArray effectsArray = new JsonArray();
        for (String[] entry : effects) {
            JsonObject eff = new JsonObject();
            eff.addProperty("effect", entry[0]);
            eff.addProperty("amplifier", Integer.parseInt(entry[1]));
            effectsArray.add(eff);
        }
        root.add("effects", effectsArray);
        if (icon != null) root.addProperty("icon", icon);
        if (iconSelected != null) root.addProperty("iconSelected", iconSelected);

        Files.writeString(dir.resolve(id + ".json"), GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static void writeReadme(Path dir) throws IOException {
        String readme = """
                === Effective Instruments — Aura Preset Format ===

                Each .json file in this folder defines one aura preset.
                The filename (without .json) becomes the aura's internal ID.

                --- Fields ---

                displayName   (string)  Display name shown in the UI
                description   (string)  Tooltip description
                color         (string)  Hex color code without 0x prefix (e.g. "FF8800")
                enabled       (bool)    Whether this aura appears in the selector
                durationTicks (int)     How long effects last in ticks (20 ticks = 1 second)
                radius        (int)     Aura range in blocks. Use -1 for the global default
                sortOrder     (int)     UI button ordering (lower = further left)
                effects       (array)   List of potion effects to apply
                  effect      (string)  Registry name of the effect (see below)
                  amplifier   (int)     Effect level minus 1 (0 = Level I, 1 = Level II, etc.)
                icon          (string)  Optional. Resource location for the button icon texture
                iconSelected  (string)  Optional. Resource location for the selected button icon

                If icon/iconSelected are omitted, the button shows the first letter
                of the display name as a fallback.

                --- Common Effect IDs ---

                minecraft:regeneration        Regeneration
                minecraft:speed               Speed
                minecraft:haste               Haste (Mining Speed)
                minecraft:strength            Strength
                minecraft:resistance          Resistance
                minecraft:absorption          Absorption
                minecraft:night_vision        Night Vision
                minecraft:fire_resistance     Fire Resistance
                minecraft:water_breathing     Water Breathing
                minecraft:jump_boost          Jump Boost
                minecraft:saturation          Saturation
                minecraft:slow_falling        Slow Falling
                minecraft:luck                Luck
                minecraft:hero_of_the_village Hero of the Village
                minecraft:conduit_power       Conduit Power
                minecraft:dolphins_grace      Dolphin's Grace

                --- Example Custom Aura ---

                Create a file called "fire_ward.json" with:
                {
                  "displayName": "Fire Ward",
                  "description": "Fire Resistance to nearby allies",
                  "color": "FF4400",
                  "enabled": true,
                  "durationTicks": 200,
                  "radius": 12,
                  "sortOrder": 10,
                  "effects": [
                    { "effect": "minecraft:fire_resistance", "amplifier": 0 }
                  ]
                }
                """;
        Files.writeString(dir.resolve("_README.txt"), readme, StandardCharsets.UTF_8);
    }

    // --- Loading ---

    public static List<AuraPreset> loadAll() {
        Path aurasDir = getAurasDir();
        if (!Files.isDirectory(aurasDir)) {
            EffectiveInstrumentsMod.LOGGER.warn("Auras directory not found: {}", aurasDir);
            return List.of();
        }

        List<AuraPreset> presets = new ArrayList<>();
        try (Stream<Path> files = Files.list(aurasDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(file -> {
                        try {
                            AuraPreset preset = parseFile(file);
                            if (preset != null) {
                                presets.add(preset);
                            }
                        } catch (Exception e) {
                            EffectiveInstrumentsMod.LOGGER.warn("Failed to parse aura file '{}': {}",
                                    file.getFileName(), e.getMessage());
                        }
                    });
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to scan auras directory", e);
        }

        // Sort by sortOrder, then alphabetically by ID
        presets.sort(Comparator.<AuraPreset>comparingInt(AuraPreset::sortOrder)
                .thenComparing(AuraPreset::id));

        return presets;
    }

    @Nullable
    private static AuraPreset parseFile(Path file) {
        String filename = file.getFileName().toString();
        String id = filename.substring(0, filename.length() - ".json".length());

        if (!id.matches("[a-z0-9_]+")) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Aura file '{}' has non-conforming ID '{}' (expected [a-z0-9_]+), skipping", filename, id);
            return null;
        }

        try {
            long fileSize = Files.size(file);
            if (fileSize > 65536) {
                EffectiveInstrumentsMod.LOGGER.warn("Aura file '{}' exceeds 64KB ({} bytes), skipping",
                        filename, fileSize);
                return null;
            }
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.warn("Could not check size of aura file '{}': {}", filename, e.getMessage());
            return null;
        }

        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.warn("Could not read aura file '{}': {}", filename, e.getMessage());
            return null;
        }

        JsonObject root;
        try {
            root = GSON.fromJson(content, JsonObject.class);
        } catch (JsonSyntaxException e) {
            EffectiveInstrumentsMod.LOGGER.warn("Invalid JSON in aura file '{}': {}", filename, e.getMessage());
            return null;
        }

        if (root == null) {
            EffectiveInstrumentsMod.LOGGER.warn("Empty aura file: '{}'", filename);
            return null;
        }

        // schemaVersion — forward-compat gate. Missing field is treated as v1.
        if (root.has(AuraSchema.FIELD)) {
            int version;
            try {
                version = root.get(AuraSchema.FIELD).getAsInt();
            } catch (Exception e) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Aura file '{}' has non-integer schemaVersion, skipping", filename);
                return null;
            }
            if (version > AuraSchema.CURRENT_VERSION) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Aura file '{}' has schemaVersion {} which is newer than this mod supports ({}), skipping",
                        filename, version, AuraSchema.CURRENT_VERSION);
                return null;
            }
        }

        // displayName
        Component displayName = parseComponent(root, "displayName", id);
        // description
        Component description = parseComponent(root, "description", "");

        // color
        int color;
        try {
            color = Integer.parseUnsignedInt(getStringOrDefault(root, "color", "FFFFFF"), 16);
        } catch (NumberFormatException e) {
            EffectiveInstrumentsMod.LOGGER.warn("Invalid color in '{}', using white", filename);
            color = 0xFFFFFF;
        }

        // enabled
        boolean enabled = root.has("enabled") ? root.get("enabled").getAsBoolean() : true;

        // durationTicks
        int durationTicks = root.has("durationTicks") ? root.get("durationTicks").getAsInt() : 160;

        // radius
        int radius = root.has("radius") ? root.get("radius").getAsInt() : -1;

        // sortOrder
        int sortOrder = root.has("sortOrder") ? root.get("sortOrder").getAsInt() : 100;

        // effects
        List<AuraPreset.EffectEntry> effects = new ArrayList<>();
        if (root.has("effects") && root.get("effects").isJsonArray()) {
            for (JsonElement elem : root.getAsJsonArray("effects")) {
                if (!elem.isJsonObject()) continue;
                JsonObject effObj = elem.getAsJsonObject();
                String effectId = getStringOrDefault(effObj, "effect", "");
                if (effectId.isEmpty()) continue;

                MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effectId));
                if (mobEffect == null) {
                    EffectiveInstrumentsMod.LOGGER.warn("Unknown effect '{}' in aura '{}', skipping", effectId, id);
                    continue;
                }

                int rawAmplifier = effObj.has("amplifier") ? effObj.get("amplifier").getAsInt() : 0;
                int amplifier = Math.max(0, Math.min(rawAmplifier, 4));
                if (rawAmplifier != amplifier) {
                    EffectiveInstrumentsMod.LOGGER.warn(
                            "Aura '{}': effect '{}' amplifier {} clamped to {} (valid range 0-4)",
                            id, effectId, rawAmplifier, amplifier);
                }
                effects.add(new AuraPreset.EffectEntry(mobEffect, amplifier));
            }
        }

        if (effects.isEmpty()) {
            EffectiveInstrumentsMod.LOGGER.warn("Aura '{}' has no valid effects, skipping", id);
            return null;
        }

        // icon (optional)
        ResourceLocation iconTexture = parseOptionalResourceLocation(root, "icon");
        ResourceLocation selectedIconTexture = parseOptionalResourceLocation(root, "iconSelected");

        // tiers (optional). Missing field → stationary-only (pre-1.3.0 behavior).
        Set<BuffTier> supportedTiers = parseTiers(root, id);

        // showInSelector (optional). Default: visible iff this preset supports the stationary tier.
        boolean showInSelector = root.has("showInSelector")
                ? root.get("showInSelector").getAsBoolean()
                : supportedTiers.contains(BuffTier.STATIONARY);

        return new AuraPreset(
                id, displayName, description, color, effects,
                durationTicks, radius, enabled, sortOrder,
                iconTexture, selectedIconTexture,
                supportedTiers, showInSelector
        );
    }

    private static Set<BuffTier> parseTiers(JsonObject root, String auraId) {
        if (!root.has("tiers") || !root.get("tiers").isJsonArray()) {
            return EnumSet.of(BuffTier.STATIONARY);
        }
        EnumSet<BuffTier> tiers = EnumSet.noneOf(BuffTier.class);
        for (JsonElement elem : root.getAsJsonArray("tiers")) {
            if (!elem.isJsonPrimitive()) continue;
            BuffTier parsed = BuffTier.fromJson(elem.getAsString());
            if (parsed != null) {
                tiers.add(parsed);
            } else {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Aura '{}' declares unknown tier '{}', ignoring",
                        auraId, elem.getAsString());
            }
        }
        if (tiers.isEmpty()) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Aura '{}' has empty 'tiers' array after parsing, defaulting to stationary", auraId);
            return EnumSet.of(BuffTier.STATIONARY);
        }
        return tiers;
    }

    private static Component parseComponent(JsonObject root, String key, String fallback) {
        if (!root.has(key)) return Component.literal(fallback);

        JsonElement elem = root.get(key);
        if (elem.isJsonPrimitive()) {
            return Component.literal(elem.getAsString());
        }
        // Attempt to parse as a full MC component (e.g. {"translate":"..."})
        try {
            Component parsed = Component.Serializer.fromJson(elem);
            return parsed != null ? parsed : Component.literal(fallback);
        } catch (Exception e) {
            return Component.literal(fallback);
        }
    }

    private static String getStringOrDefault(JsonObject obj, String key, String def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : def;
    }

    @Nullable
    private static ResourceLocation parseOptionalResourceLocation(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) return null;
        String val = obj.get(key).getAsString();
        if (val.isEmpty()) return null;
        return ResourceLocation.isValidResourceLocation(val) ? new ResourceLocation(val) : null;
    }

    private AuraJsonLoader() {}
}

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

    // --- Default generation ---

    public static void ensureDefaults() {
        Path marker = getMarkerFile();
        if (Files.exists(marker)) return;

        Path aurasDir = getAurasDir();
        try {
            Files.createDirectories(aurasDir);

            // --- Genshin Instruments auras ---

            writeDefaultJson(aurasDir, "zephyrs_blessing", "Zephyr's Blessing",
                    "A gentle breeze quickens the step of nearby allies", "7FDBCA", 160, 0,
                    new String[][]{{"minecraft:speed", "0"}});

            writeDefaultJson(aurasDir, "echoes_of_antiquity", "Echoes of Antiquity",
                    "Ancient melodies mend the wounds of nearby allies", "D4A574", 160, 1,
                    new String[][]{{"minecraft:regeneration", "0"}});

            writeDefaultJson(aurasDir, "bloom_veil", "Bloom Veil",
                    "Nourishes and shields nearby allies with floral energy", "FF88CC", 160, 2,
                    new String[][]{{"minecraft:absorption", "0"}, {"minecraft:saturation", "0"}});

            writeDefaultJson(aurasDir, "warcry_cadence", "Warcry Cadence",
                    "Emboldens nearby allies with strength and resilience", "FF4422", 160, 3,
                    new String[][]{{"minecraft:strength", "0"}, {"minecraft:resistance", "0"}});

            writeDefaultJson(aurasDir, "moonlit_passage", "Moonlit Passage",
                    "Grants night sight and safe descent to nearby allies", "6644BB", 260, 4,
                    new String[][]{{"minecraft:night_vision", "0"}, {"minecraft:slow_falling", "0"}});

            writeDefaultJson(aurasDir, "sunkissed_serenade", "Sunkissed Serenade",
                    "Fortune favors nearby allies with improved luck", "FFDD44", 200, 5,
                    new String[][]{{"minecraft:luck", "0"}});

            writeDefaultJson(aurasDir, "rhythm_of_the_earth", "Rhythm of the Earth",
                    "Primal beats hasten the hands and spring the step of nearby allies", "CC6633", 160, 6,
                    new String[][]{{"minecraft:haste", "0"}, {"minecraft:jump_boost", "0"}});

            // --- Even More Instruments auras ---

            writeDefaultJson(aurasDir, "wanderers_anthem", "Wanderer's Anthem",
                    "Quickens stride and lightens step for nearby allies", "B8860B", 160, 7,
                    new String[][]{{"minecraft:speed", "0"}, {"minecraft:jump_boost", "0"}});

            writeDefaultJson(aurasDir, "harmonic_resonance", "Harmonic Resonance",
                    "Precise harmonies restore and energize nearby allies", "EEEEFF", 160, 8,
                    new String[][]{{"minecraft:regeneration", "0"}, {"minecraft:haste", "0"}});

            writeDefaultJson(aurasDir, "tranquil_current", "Tranquil Current",
                    "Flowing tones grant water breathing and aquatic grace to nearby allies", "44AACC", 260, 9,
                    new String[][]{{"minecraft:water_breathing", "0"}, {"minecraft:dolphins_grace", "0"}});

            writeDefaultJson(aurasDir, "silk_road_vigor", "Silk Road Vigor",
                    "Fierce melodies drive nearby allies to move and strike with vigor", "FF6644", 160, 10,
                    new String[][]{{"minecraft:speed", "0"}, {"minecraft:strength", "0"}});

            writeDefaultJson(aurasDir, "smoky_allure", "Smoky Allure",
                    "Charismatic jazz melodies charm villagers into offering better trades", "DAA520", 200, 11,
                    new String[][]{{"minecraft:hero_of_the_village", "0"}});

            writeDefaultJson(aurasDir, "ghost_flame", "Ghost Flame",
                    "Spectral fire shields from flame and emboldens nearby allies", "88CCFF", 200, 12,
                    new String[][]{{"minecraft:fire_resistance", "0"}, {"minecraft:strength", "0"}});

            writeDefaultJson(aurasDir, "bulwark_fanfare", "Bulwark Fanfare",
                    "Triumphant brass fortifies and shields nearby allies", "CC8800", 160, 13,
                    new String[][]{{"minecraft:resistance", "0"}, {"minecraft:absorption", "1"}});

            writeDefaultJson(aurasDir, "heartstring_aria", "Heartstring Aria",
                    "A soaring melody that heals and shields nearby allies", "CC4466", 200, 14,
                    new String[][]{{"minecraft:regeneration", "0"}, {"minecraft:absorption", "0"}});

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

    private static void writeDefaultJson(Path dir, String id, String displayName, String description,
                                          String color, int durationTicks, int sortOrder,
                                          String[][] effects, String icon, String iconSelected) throws IOException {
        JsonObject root = new JsonObject();
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

                int amplifier = effObj.has("amplifier") ? effObj.get("amplifier").getAsInt() : 0;
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

        return new AuraPreset(
                id, displayName, description, color, effects,
                durationTicks, radius, enabled, sortOrder,
                iconTexture, selectedIconTexture
        );
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

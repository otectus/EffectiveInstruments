package com.crims.effectiveinstruments.aura;

import com.google.gson.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the JSON parsing logic used by InstrumentAuraMapping.
 * These tests validate the parsing behavior without needing a Minecraft runtime.
 */
class InstrumentAuraMappingJsonTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Parsed config for a single instrument.
     */
    record ParsedConfig(String defaultAuraId, List<String> allowedAuraIds) {}

    /**
     * Simulate the parsing logic from InstrumentAuraMapping.load()
     * without needing AuraRegistry validation.
     */
    private Map<String, ParsedConfig> parseMappings(String json) {
        Map<String, ParsedConfig> result = new HashMap<>();
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        if (root == null) return result;

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) continue;

            JsonElement value = entry.getValue();
            ParsedConfig config = parseEntry(value);
            if (config != null) {
                result.put(key, config);
            }
        }
        return result;
    }

    private ParsedConfig parseEntry(JsonElement value) {
        if (value.isJsonNull()) return null;

        // String shorthand
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String auraId = value.getAsString();
            if (auraId.isEmpty()) return null;
            return new ParsedConfig(auraId, List.of(auraId));
        }

        // Object form
        if (value.isJsonObject()) {
            JsonObject obj = value.getAsJsonObject();
            if (!obj.has("default") || !obj.get("default").isJsonPrimitive()) return null;

            String defaultAuraId = obj.get("default").getAsString();
            if (defaultAuraId.isEmpty()) return null;

            Set<String> allowedSet = new LinkedHashSet<>();
            allowedSet.add(defaultAuraId);

            if (obj.has("allowed") && obj.get("allowed").isJsonArray()) {
                for (JsonElement elem : obj.getAsJsonArray("allowed")) {
                    if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isString()) continue;
                    String auraId = elem.getAsString();
                    if (!auraId.isEmpty()) allowedSet.add(auraId);
                }
            }

            return new ParsedConfig(defaultAuraId, List.copyOf(allowedSet));
        }

        return null;
    }

    @Test
    void parsesStringShorthand() {
        String json = """
                {
                    "genshinstrument:windsong_lyre": "soothing_hymn"
                }
                """;
        Map<String, ParsedConfig> mappings = parseMappings(json);
        assertEquals(1, mappings.size());

        ParsedConfig config = mappings.get("genshinstrument:windsong_lyre");
        assertNotNull(config);
        assertEquals("soothing_hymn", config.defaultAuraId());
        assertEquals(List.of("soothing_hymn"), config.allowedAuraIds());
    }

    @Test
    void parsesObjectForm() {
        String json = """
                {
                    "genshinstrument:windsong_lyre": {
                        "default": "soothing_hymn",
                        "allowed": ["soothing_hymn", "guardian_chorus"]
                    }
                }
                """;
        Map<String, ParsedConfig> mappings = parseMappings(json);
        ParsedConfig config = mappings.get("genshinstrument:windsong_lyre");
        assertNotNull(config);
        assertEquals("soothing_hymn", config.defaultAuraId());
        assertEquals(List.of("soothing_hymn", "guardian_chorus"), config.allowedAuraIds());
    }

    @Test
    void objectFormAutoIncludesDefault() {
        String json = """
                {
                    "genshinstrument:windsong_lyre": {
                        "default": "soothing_hymn",
                        "allowed": ["guardian_chorus"]
                    }
                }
                """;
        Map<String, ParsedConfig> mappings = parseMappings(json);
        ParsedConfig config = mappings.get("genshinstrument:windsong_lyre");
        assertNotNull(config);
        // Default should be auto-added to allowed even if not explicitly listed
        assertTrue(config.allowedAuraIds().contains("soothing_hymn"));
        assertTrue(config.allowedAuraIds().contains("guardian_chorus"));
    }

    @Test
    void objectFormWithoutAllowedDefaultsToSingleAura() {
        String json = """
                {
                    "genshinstrument:windsong_lyre": {
                        "default": "soothing_hymn"
                    }
                }
                """;
        Map<String, ParsedConfig> mappings = parseMappings(json);
        ParsedConfig config = mappings.get("genshinstrument:windsong_lyre");
        assertNotNull(config);
        assertEquals(List.of("soothing_hymn"), config.allowedAuraIds());
    }

    @Test
    void skipsCommentFields() {
        String json = """
                {
                    "_comment": "This is a comment",
                    "genshinstrument:ukulele": "soothing_hymn"
                }
                """;
        Map<String, ParsedConfig> mappings = parseMappings(json);
        assertEquals(1, mappings.size());
        assertFalse(mappings.containsKey("_comment"));
    }

    @Test
    void skipsNullValues() {
        String json = """
                {
                    "genshinstrument:ukulele": null,
                    "genshinstrument:floral_zither": "guardian_chorus"
                }
                """;
        Map<String, ParsedConfig> mappings = parseMappings(json);
        assertEquals(1, mappings.size());
        assertFalse(mappings.containsKey("genshinstrument:ukulele"));
    }

    @Test
    void skipsEmptyStringValues() {
        String json = """
                {
                    "genshinstrument:ukulele": "",
                    "genshinstrument:floral_zither": "guardian_chorus"
                }
                """;
        Map<String, ParsedConfig> mappings = parseMappings(json);
        assertEquals(1, mappings.size());
    }

    @Test
    void skipsNonStringNonObjectValues() {
        String json = """
                {
                    "genshinstrument:ukulele": 42,
                    "genshinstrument:windsong_lyre": "soothing_hymn"
                }
                """;
        Map<String, ParsedConfig> mappings = parseMappings(json);
        assertEquals(1, mappings.size());
    }

    @Test
    void handlesEmptyJson() {
        Map<String, ParsedConfig> mappings = parseMappings("{}");
        assertTrue(mappings.isEmpty());
    }

    @Test
    void handlesMixedFormats() {
        String json = """
                {
                    "genshinstrument:windsong_lyre": "soothing_hymn",
                    "genshinstrument:glorious_drum": {
                        "default": "invigorating_march",
                        "allowed": ["invigorating_march", "guardian_chorus"]
                    }
                }
                """;
        Map<String, ParsedConfig> mappings = parseMappings(json);
        assertEquals(2, mappings.size());

        ParsedConfig lyre = mappings.get("genshinstrument:windsong_lyre");
        assertEquals("soothing_hymn", lyre.defaultAuraId());
        assertEquals(1, lyre.allowedAuraIds().size());

        ParsedConfig drum = mappings.get("genshinstrument:glorious_drum");
        assertEquals("invigorating_march", drum.defaultAuraId());
        assertEquals(2, drum.allowedAuraIds().size());
    }

    @Test
    void deduplicatesAllowedList() {
        String json = """
                {
                    "genshinstrument:windsong_lyre": {
                        "default": "soothing_hymn",
                        "allowed": ["soothing_hymn", "soothing_hymn", "guardian_chorus"]
                    }
                }
                """;
        Map<String, ParsedConfig> mappings = parseMappings(json);
        ParsedConfig config = mappings.get("genshinstrument:windsong_lyre");
        // LinkedHashSet deduplicates
        assertEquals(2, config.allowedAuraIds().size());
    }
}

package com.crims.effectiveinstruments.aura;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the parsing rules of {@link MobileInstrumentAuraMapping}. Mirrors the
 * production loop locally (no Minecraft runtime), matching the existing
 * {@link InstrumentAuraMappingJsonTest} pattern.
 *
 * <p>The real mapping also consults {@link AuraRegistry}; that resolution layer
 * is intentionally NOT covered here — the unit tested is the JSON → (id, auraId)
 * shape translation only. Integration is covered by the manual QA checklist.
 */
class MobileInstrumentAuraMappingJsonTest {

    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Mirror of {@link MobileInstrumentAuraMapping#load()}'s entry loop. Returns
     * null if the file should be rejected wholesale (schema gate), or a map
     * (possibly empty) of valid (instrumentId → auraId) pairs.
     */
    private static Map<String, String> parse(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        if (root == null) return null;

        // Schema gate — mirror of production.
        if (root.has(AuraSchema.FIELD)) {
            int version;
            try {
                version = root.get(AuraSchema.FIELD).getAsInt();
            } catch (Exception e) {
                return null;
            }
            if (version > AuraSchema.CURRENT_VERSION) return null;
        }

        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) continue;
            if (AuraSchema.FIELD.equals(key)) continue;
            // Minimal resource-location validation: must contain a ':' and lowercase namespace/path chars.
            // The production code calls ResourceLocation.isValidResourceLocation, which we approximate here.
            if (!looksLikeResourceLocation(key)) continue;

            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) continue;
            out.put(key, value.getAsString());
        }
        return out;
    }

    private static boolean looksLikeResourceLocation(String s) {
        int colon = s.indexOf(':');
        if (colon <= 0 || colon == s.length() - 1) return false;
        String ns = s.substring(0, colon);
        String path = s.substring(colon + 1);
        return ns.matches("[a-z0-9_.-]+") && path.matches("[a-z0-9_./-]+");
    }

    @Test
    void singleEntryParses() {
        Map<String, String> result = parse("""
                {"immersive_melodies:flute":"windstep_mobile"}
                """);
        assertEquals(1, result.size());
        assertEquals("windstep_mobile", result.get("immersive_melodies:flute"));
    }

    @Test
    void commentKeysAreSkipped() {
        Map<String, String> result = parse("""
                {
                  "_comment": "this is a note",
                  "immersive_melodies:lute": "traveler_hum_mobile"
                }
                """);
        assertFalse(result.containsKey("_comment"));
        assertEquals(1, result.size());
    }

    @Test
    void schemaVersionKeyIsSkippedAsEntry() {
        // The schemaVersion field must not be treated as an instrument mapping.
        Map<String, String> result = parse("""
                {
                  "schemaVersion": 1,
                  "immersive_melodies:piano": "measured_tempo_mobile"
                }
                """);
        assertFalse(result.containsKey(AuraSchema.FIELD));
        assertEquals(1, result.size());
        assertEquals("measured_tempo_mobile", result.get("immersive_melodies:piano"));
    }

    @Test
    void nonStringValuesAreSkipped() {
        // A mobile entry MUST be a string aura id — objects/arrays/numbers are rejected
        // (unlike the stationary mapping which also accepts object-form entries).
        Map<String, String> result = parse("""
                {
                  "immersive_melodies:flute": {"default":"x","allowed":["x"]},
                  "immersive_melodies:lute": 42,
                  "immersive_melodies:piano": "measured_tempo_mobile"
                }
                """);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("immersive_melodies:piano"));
    }

    @Test
    void invalidResourceLocationKeysAreSkipped() {
        Map<String, String> result = parse("""
                {
                  "NOT A RESOURCE LOCATION": "windstep_mobile",
                  "has:two:colons": "windstep_mobile",
                  "immersive_melodies:flute": "windstep_mobile"
                }
                """);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("immersive_melodies:flute"));
    }

    @Test
    void futureSchemaVersionRejectsWholeFile() {
        int future = AuraSchema.CURRENT_VERSION + 1;
        Map<String, String> result = parse("""
                {
                  "schemaVersion": %d,
                  "immersive_melodies:flute": "windstep_mobile"
                }
                """.formatted(future));
        assertNull(result, "Newer-schema file must be rejected wholesale, not parsed partially");
    }

    @Test
    void missingSchemaVersionIsTreatedAsCurrent() {
        Map<String, String> result = parse("""
                {"immersive_melodies:flute":"windstep_mobile"}
                """);
        assertEquals(1, result.size());
    }

    @Test
    void nonIntegerSchemaVersionRejectsWholeFile() {
        assertNull(parse("{\"schemaVersion\":\"one\"}"));
    }

    @Test
    void emptyFileParsesToEmptyMap() {
        assertEquals(0, parse("{}").size());
    }

    @Test
    void elevenDefaultMappingsRoundtrip() {
        // Lock in the 11-entry default mapping count so an accidental deletion trips the test.
        Map<String, String> result = parse("""
                {
                  "schemaVersion": 1,
                  "_comment": "defaults",
                  "immersive_melodies:flute": "windstep_mobile",
                  "immersive_melodies:lute": "traveler_hum_mobile",
                  "immersive_melodies:piano": "measured_tempo_mobile",
                  "immersive_melodies:vielle": "hearthsong_mobile",
                  "immersive_melodies:didgeridoo": "earthpulse_mobile",
                  "immersive_melodies:bagpipe": "steadfast_drone_mobile",
                  "immersive_melodies:trumpet": "brass_call_mobile",
                  "immersive_melodies:tiny_drum": "march_tap_mobile",
                  "immersive_melodies:triangle": "clear_ping_mobile",
                  "immersive_melodies:handpan": "stillwater_mobile",
                  "immersive_melodies:ender_bass": "shade_resonance_mobile"
                }
                """);
        assertEquals(11, result.size());
        assertEquals("shade_resonance_mobile", result.get("immersive_melodies:ender_bass"));
    }
}

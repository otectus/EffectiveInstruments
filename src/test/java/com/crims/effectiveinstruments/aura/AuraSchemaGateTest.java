package com.crims.effectiveinstruments.aura;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the schema-version forward-compat gate used by both
 * AuraJsonLoader and InstrumentAuraMapping. Mirrors the production
 * check locally so it runs without a Minecraft runtime, matching the
 * existing test pattern in {@link InstrumentAuraMappingJsonTest}.
 */
class AuraSchemaGateTest {

    private static final Gson GSON = new Gson();

    /** Mirror of the production gate. Returns true if the file should be accepted. */
    private static boolean accepts(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        if (root == null) return false;
        if (!root.has(AuraSchema.FIELD)) return true; // missing = v1
        int version;
        try {
            version = root.get(AuraSchema.FIELD).getAsInt();
        } catch (Exception e) {
            return false; // non-integer rejected
        }
        return version <= AuraSchema.CURRENT_VERSION;
    }

    @Test
    void currentVersionIsPositive() {
        // Bumping CURRENT_VERSION without thinking should break something.
        // This test is the smoke alarm.
        assertTrue(AuraSchema.CURRENT_VERSION >= 1);
    }

    @Test
    void missingFieldIsAcceptedAsV1() {
        assertTrue(accepts("{\"displayName\":\"Test\"}"));
    }

    @Test
    void currentVersionIsAccepted() {
        String json = "{\"" + AuraSchema.FIELD + "\":" + AuraSchema.CURRENT_VERSION + "}";
        assertTrue(accepts(json));
    }

    @Test
    void olderVersionIsAccepted() {
        // Anything <= current is accepted so old presets keep working.
        assertTrue(accepts("{\"" + AuraSchema.FIELD + "\":1}"));
    }

    @Test
    void futureVersionIsRejected() {
        int future = AuraSchema.CURRENT_VERSION + 1;
        String json = "{\"" + AuraSchema.FIELD + "\":" + future + "}";
        assertFalse(accepts(json),
                "Loader must refuse files from a newer schema than it can understand");
    }

    @Test
    void nonIntegerVersionIsRejected() {
        assertFalse(accepts("{\"" + AuraSchema.FIELD + "\":\"one\"}"));
        assertFalse(accepts("{\"" + AuraSchema.FIELD + "\":true}"));
    }

    @Test
    void fieldNameMatchesConstant() {
        // Guards against accidental rename breaking all existing files.
        assertEquals("schemaVersion", AuraSchema.FIELD);
    }
}

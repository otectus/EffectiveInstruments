package com.crims.effectiveinstruments.aura;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the tier/selector parsing rules added in 1.3.0.
 * Mirrors {@link AuraJsonLoader#parseTiers} + showInSelector defaulting locally
 * so the test runs without a Minecraft runtime, matching the existing pattern
 * in {@link AuraSchemaGateTest}.
 */
class AuraTierJsonTest {

    private static final Gson GSON = new Gson();

    /** Mirror of production parseTiers(). Empty/missing → stationary-only. */
    private static Set<BuffTier> parseTiers(JsonObject root) {
        if (!root.has("tiers") || !root.get("tiers").isJsonArray()) {
            return EnumSet.of(BuffTier.STATIONARY);
        }
        EnumSet<BuffTier> tiers = EnumSet.noneOf(BuffTier.class);
        for (JsonElement elem : root.getAsJsonArray("tiers")) {
            if (!elem.isJsonPrimitive()) continue;
            BuffTier parsed = BuffTier.fromJson(elem.getAsString());
            if (parsed != null) tiers.add(parsed);
        }
        if (tiers.isEmpty()) return EnumSet.of(BuffTier.STATIONARY);
        return tiers;
    }

    /** Mirror of production showInSelector defaulting. */
    private static boolean parseShowInSelector(JsonObject root, Set<BuffTier> tiers) {
        if (root.has("showInSelector")) return root.get("showInSelector").getAsBoolean();
        return tiers.contains(BuffTier.STATIONARY);
    }

    private static JsonObject obj(String json) {
        return GSON.fromJson(json, JsonObject.class);
    }

    @Test
    void missingTiersFieldDefaultsToStationary() {
        // Back-compat: every pre-1.3.0 aura JSON must parse as stationary-only.
        Set<BuffTier> tiers = parseTiers(obj("{\"displayName\":\"Old\"}"));
        assertEquals(EnumSet.of(BuffTier.STATIONARY), tiers);
    }

    @Test
    void mobileOnlyParses() {
        Set<BuffTier> tiers = parseTiers(obj("{\"tiers\":[\"mobile\"]}"));
        assertEquals(EnumSet.of(BuffTier.MOBILE), tiers);
        assertFalse(tiers.contains(BuffTier.STATIONARY));
    }

    @Test
    void bothTiersParse() {
        Set<BuffTier> tiers = parseTiers(obj("{\"tiers\":[\"stationary\",\"mobile\"]}"));
        assertEquals(EnumSet.allOf(BuffTier.class), tiers);
    }

    @Test
    void unknownTierStringsAreIgnoredButDontBlankTheSet() {
        // Mixed valid + garbage: keep the valid entries, drop the garbage, don't fall back to default.
        Set<BuffTier> tiers = parseTiers(obj("{\"tiers\":[\"mobile\",\"future_tier\",\"NOPE\"]}"));
        assertEquals(EnumSet.of(BuffTier.MOBILE), tiers);
    }

    @Test
    void allUnknownTiersFallBackToStationary() {
        // If every element is bogus the result is an empty set; we fall back to the safe default.
        Set<BuffTier> tiers = parseTiers(obj("{\"tiers\":[\"garbage\"]}"));
        assertEquals(EnumSet.of(BuffTier.STATIONARY), tiers);
    }

    @Test
    void tierParsingIsCaseInsensitive() {
        assertEquals(EnumSet.of(BuffTier.MOBILE), parseTiers(obj("{\"tiers\":[\"MOBILE\"]}")));
        assertEquals(EnumSet.of(BuffTier.STATIONARY), parseTiers(obj("{\"tiers\":[\"Stationary\"]}")));
    }

    @Test
    void showInSelectorDefaultsTrueForStationaryPresets() {
        // Default visibility for a stationary preset with no explicit flag is visible.
        Set<BuffTier> tiers = EnumSet.of(BuffTier.STATIONARY);
        assertTrue(parseShowInSelector(obj("{}"), tiers));
    }

    @Test
    void showInSelectorDefaultsFalseForMobileOnlyPresets() {
        // Mobile-only presets should not leak into the stationary selector UI.
        Set<BuffTier> tiers = EnumSet.of(BuffTier.MOBILE);
        assertFalse(parseShowInSelector(obj("{}"), tiers));
    }

    @Test
    void showInSelectorExplicitFalseWins() {
        Set<BuffTier> tiers = EnumSet.of(BuffTier.STATIONARY);
        assertFalse(parseShowInSelector(obj("{\"showInSelector\":false}"), tiers));
    }

    @Test
    void showInSelectorExplicitTrueWinsEvenForMobileOnly() {
        // Weird but allowed: author forces a mobile preset to be selectable.
        // This test locks in "explicit wins" semantics so changing the default
        // doesn't silently flip meaning.
        Set<BuffTier> tiers = EnumSet.of(BuffTier.MOBILE);
        assertTrue(parseShowInSelector(obj("{\"showInSelector\":true}"), tiers));
    }

    @Test
    void dualTierDefaultsToVisibleBecauseStationaryPresent() {
        Set<BuffTier> tiers = EnumSet.allOf(BuffTier.class);
        assertTrue(parseShowInSelector(obj("{}"), tiers));
    }

    @Test
    void buffTierEnumHasExpectedConstants() {
        // Freezes the enum shape — adding a new tier should be a conscious decision,
        // not a silent import that breaks unrelated switch statements.
        assertEquals(2, BuffTier.values().length);
        assertEquals(BuffTier.STATIONARY, BuffTier.fromJson("stationary"));
        assertEquals(BuffTier.MOBILE, BuffTier.fromJson("mobile"));
    }

    @Test
    void buffTierFromJsonRejectsUnknown() {
        assertEquals(null, BuffTier.fromJson(""));
        assertEquals(null, BuffTier.fromJson(null));
        assertEquals(null, BuffTier.fromJson("stationary_ish"));
    }

    @Test
    void mobileOnlyArrayIsNotJustStationary() {
        // Regression guard: a naive parseTiers that swallows everything to STATIONARY
        // would pass missingTiersFieldDefaultsToStationary() but fail this one.
        JsonArray arr = new JsonArray();
        arr.add("mobile");
        JsonObject root = new JsonObject();
        root.add("tiers", arr);
        Set<BuffTier> tiers = parseTiers(root);
        assertFalse(tiers.contains(BuffTier.STATIONARY));
        assertTrue(tiers.contains(BuffTier.MOBILE));
    }
}

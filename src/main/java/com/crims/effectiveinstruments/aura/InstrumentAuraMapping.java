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
     * MobEffect pairing used by {@link #findOffensiveByEffect} when a user's
     * custom positive aura doesn't appear in {@link #POSITIVE_TO_OFFENSIVE_AURA_ID}.
     * Maps a positive effect ID to its canonical debuff counterpart — the
     * synthesis then scans the registry for an offensive preset whose first
     * effect matches. Left-hand side is the positive MobEffect's registry id.
     */
    private static final Map<String, String> POSITIVE_EFFECT_INVERSE = Map.ofEntries(
            Map.entry("minecraft:speed",                "minecraft:slowness"),
            Map.entry("minecraft:jump_boost",           "minecraft:slowness"),
            Map.entry("minecraft:haste",                "minecraft:mining_fatigue"),
            Map.entry("minecraft:strength",             "minecraft:weakness"),
            Map.entry("minecraft:regeneration",         "minecraft:wither"),
            Map.entry("minecraft:saturation",           "minecraft:hunger"),
            Map.entry("minecraft:absorption",           "minecraft:wither"),
            Map.entry("minecraft:resistance",           "minecraft:weakness"),
            Map.entry("minecraft:night_vision",         "minecraft:blindness"),
            Map.entry("minecraft:water_breathing",      "minecraft:nausea"),
            Map.entry("minecraft:dolphins_grace",       "minecraft:nausea"),
            Map.entry("minecraft:fire_resistance",      "minecraft:wither"),
            Map.entry("minecraft:slow_falling",         "minecraft:levitation"),
            Map.entry("minecraft:luck",                 "minecraft:nausea"),
            Map.entry("minecraft:hero_of_the_village",  "minecraft:weakness")
    );

    /**
     * Static pairing from positive stationary aura IDs to their offensive
     * counterpart. Used by {@link #ensureOffensiveAllowedLists()} and the 1.4.4
     * {@link #ensureUniqueAssignment()} migration. 31 entries — one per shipped
     * instrument.
     */
    private static final Map<String, String> POSITIVE_TO_OFFENSIVE_AURA_ID = Map.ofEntries(
            Map.entry("zephyrs_blessing", "zephyrs_wrath"),
            Map.entry("echoes_of_antiquity", "echoes_of_decay"),
            Map.entry("bloom_veil", "withering_bloom"),
            Map.entry("warcry_cadence", "battle_fanfare"),
            Map.entry("moonlit_passage", "ebon_dirge"),
            Map.entry("sunkissed_serenade", "mischief_melody"),
            Map.entry("rhythm_of_the_earth", "earth_tremor"),
            Map.entry("wanderers_anthem", "storm_drifter"),
            Map.entry("harmonic_resonance", "discordant_chord"),
            Map.entry("tranquil_current", "fathomless_pull"),
            Map.entry("silk_road_vigor", "winds_of_war"),
            Map.entry("smoky_allure", "bleeding_note"),
            Map.entry("ghost_flame", "spectral_wail"),
            Map.entry("bulwark_fanfare", "obsidian_blast"),
            Map.entry("heartstring_aria", "dirge_of_grief"),
            // 1.4.4 expansion — 16 new pairs to cover the EMI note-block variants.
            Map.entry("skyward_zephyr", "skyward_blight"),
            Map.entry("rumbling_anthem", "rumbling_curse"),
            Map.entry("thunderous_cadence", "thunderous_dirge"),
            Map.entry("drumline_vigor", "drumline_blight"),
            Map.entry("artisan_tempo", "artisan_curse"),
            Map.entry("chiming_revival", "tolling_entropy"),
            Map.entry("starlit_grace", "starlit_malice"),
            Map.entry("fleetfoot_lilt", "fleetfoot_fall"),
            Map.entry("troubadour_march", "troubadour_dirge"),
            Map.entry("craftwork_rondo", "craftwork_rot"),
            Map.entry("ironwright_anthem", "ironwright_curse"),
            Map.entry("pasture_serenade", "pasture_rot"),
            Map.entry("hearthlight_drone", "hearthshade_dirge"),
            Map.entry("pixel_pulse", "pixel_rot"),
            Map.entry("wayfinders_reel", "wayfinders_lament"),
            Map.entry("bellwether_toll", "bellwether_rot")
    );

    /**
     * 1.4.4: canonical 1-to-1 mapping from instrument id → positive aura id.
     * Every instrument has a UNIQUE positive aura. The offensive counterpart
     * is derived via {@link #POSITIVE_TO_OFFENSIVE_AURA_ID}. Both fresh-install
     * default generation and the {@link #ensureUniqueAssignment()} migration
     * consume this table.
     */
    private static final Map<String, String> UNIQUE_INSTRUMENT_TO_POSITIVE = Map.ofEntries(
            // Genshin Instruments (7)
            Map.entry("genshinstrument:windsong_lyre", "zephyrs_blessing"),
            Map.entry("genshinstrument:vintage_lyre", "echoes_of_antiquity"),
            Map.entry("genshinstrument:floral_zither", "bloom_veil"),
            Map.entry("genshinstrument:glorious_drum", "warcry_cadence"),
            Map.entry("genshinstrument:nightwind_horn", "moonlit_passage"),
            Map.entry("genshinstrument:ukulele", "sunkissed_serenade"),
            Map.entry("genshinstrument:djem_djem_drum", "rhythm_of_the_earth"),
            // Even More Instruments core (8)
            Map.entry("evenmoreinstruments:guitar", "wanderers_anthem"),
            Map.entry("evenmoreinstruments:keyboard", "harmonic_resonance"),
            Map.entry("evenmoreinstruments:koto", "tranquil_current"),
            Map.entry("evenmoreinstruments:pipa", "silk_road_vigor"),
            Map.entry("evenmoreinstruments:saxophone", "smoky_allure"),
            Map.entry("evenmoreinstruments:shamisen", "ghost_flame"),
            Map.entry("evenmoreinstruments:trombone", "bulwark_fanfare"),
            Map.entry("evenmoreinstruments:violin", "heartstring_aria"),
            // Even More Instruments note-block variants (16) — each now unique.
            Map.entry("evenmoreinstruments:harp_note_block_instrument", "skyward_zephyr"),
            Map.entry("evenmoreinstruments:bass_note_block_instrument", "rumbling_anthem"),
            Map.entry("evenmoreinstruments:basedrum_note_block_instrument", "thunderous_cadence"),
            Map.entry("evenmoreinstruments:snare_note_block_instrument", "drumline_vigor"),
            Map.entry("evenmoreinstruments:hat_note_block_instrument", "artisan_tempo"),
            Map.entry("evenmoreinstruments:bell_note_block_instrument", "chiming_revival"),
            Map.entry("evenmoreinstruments:chime_note_block_instrument", "starlit_grace"),
            Map.entry("evenmoreinstruments:flute_note_block_instrument", "fleetfoot_lilt"),
            Map.entry("evenmoreinstruments:guitar_note_block_instrument", "troubadour_march"),
            Map.entry("evenmoreinstruments:xylophone_note_block_instrument", "craftwork_rondo"),
            Map.entry("evenmoreinstruments:iron_xylophone_note_block_instrument", "ironwright_anthem"),
            Map.entry("evenmoreinstruments:cow_bell_note_block_instrument", "pasture_serenade"),
            Map.entry("evenmoreinstruments:didgeridoo_note_block_instrument", "hearthlight_drone"),
            Map.entry("evenmoreinstruments:bit_note_block_instrument", "pixel_pulse"),
            Map.entry("evenmoreinstruments:banjo_note_block_instrument", "wayfinders_reel"),
            Map.entry("evenmoreinstruments:pling_note_block_instrument", "bellwether_toll")
    );

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

    /**
     * Distinct marker for the 1.4.0 offensive-aura allow-list migration. Separate
     * from {@link #getMarkerFile()} so upgraded installs (which already have the
     * 1.x marker) still pick up the new allowed entries on next boot.
     */
    private static Path getOffensiveMigrationMarker() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.instrument_offensive_migration_done");
    }

    /**
     * 1.4.4 migration marker. Once per install, walks the existing mapping file
     * and reassigns any instrument whose current default aura is one of the
     * seven 1.4.x-duplicated ids (harmonic_resonance, sunkissed_serenade, etc.)
     * to its 1.4.4 unique assignment from {@link #UNIQUE_INSTRUMENT_TO_POSITIVE}.
     * Leaves user-customized entries (auras outside the known set) alone.
     *
     * <p>Bumped to {@code _v2} in 1.4.8 so installs that ran the 1.4.4 migration
     * once but still have duplicates (via effect-based synthesis producing the
     * same offensive id for multiple custom positives) re-run the migration
     * with the now-uniqueness-aware synthesis path.
     */
    private static Path getUniqueAssignmentMarker() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.instrument_unique_assignment_v2_done");
    }

    // --- Default generation ---

    public static void ensureDefaults() {
        ensureFirstRunDefaults();
        ensureOffensiveAllowedLists();
        ensureUniqueAssignment();
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
                    "Maps instrument IDs to aura configs. "
                            + "String value = single aura (default + only allowed). "
                            + "Object value = {\"default\": \"aura_id\", \"allowed\": [\"aura1\", \"aura2\"]}. "
                            + "Remove an entry to show all auras for that instrument. "
                            + "Reload with /effectiveinstruments reload.");
            // 1.4.4: 1-to-1 unique assignment. Every instrument maps to its own
            // positive aura + the corresponding offensive counterpart. Source of
            // truth: UNIQUE_INSTRUMENT_TO_POSITIVE + POSITIVE_TO_OFFENSIVE_AURA_ID.
            for (Map.Entry<String, String> e : UNIQUE_INSTRUMENT_TO_POSITIVE.entrySet()) {
                String positive = e.getValue();
                String offensive = POSITIVE_TO_OFFENSIVE_AURA_ID.get(positive);
                if (offensive == null) {
                    EffectiveInstrumentsMod.LOGGER.warn(
                            "Instrument '{}' positive '{}' has no offensive counterpart — shipping as single-aura entry",
                            e.getKey(), positive);
                    JsonObject entry = new JsonObject();
                    entry.addProperty("default", positive);
                    JsonArray allowed = new JsonArray();
                    allowed.add(positive);
                    entry.add("allowed", allowed);
                    root.add(e.getKey(), entry);
                    continue;
                }
                addInstrumentEntry(root, e.getKey(), positive, offensive);
            }

            Files.writeString(mappingFile, GSON.toJson(root), StandardCharsets.UTF_8);
            writeReadme(mappingFile.getParent());
            Files.createFile(marker);
            // Mark the offensive migration and 1.4.4 unique-assignment as already
            // done — fresh installs ship with the full 1-to-1 mapping.
            Path offensiveMarker = getOffensiveMigrationMarker();
            if (!Files.exists(offensiveMarker)) {
                Files.createFile(offensiveMarker);
            }
            Path uniqueMarker = getUniqueAssignmentMarker();
            if (!Files.exists(uniqueMarker)) {
                Files.createFile(uniqueMarker);
            }

            EffectiveInstrumentsMod.LOGGER.info("Generated default instrument-aura mappings");
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to generate default instrument-aura mappings", e);
        }
    }

    /**
     * Helper used by fresh-install default generation: emits an object-form entry
     * {@code {"default": positive, "allowed": [positive, offensive]}} so the
     * selector shows both choices out of the box.
     */
    private static void addInstrumentEntry(JsonObject root, String instrumentId, String positive, String offensive) {
        JsonObject entry = new JsonObject();
        entry.addProperty("default", positive);
        JsonArray allowed = new JsonArray();
        allowed.add(positive);
        allowed.add(offensive);
        entry.add("allowed", allowed);
        root.add(instrumentId, entry);
    }

    /**
     * One-shot 1.4.0 migration: walks the existing mapping file, and for each
     * string-form entry whose value is a known 1.3.x positive aura ID, rewrites
     * the entry to object form with the matching offensive aura added to
     * {@code allowed}. Object-form entries and unknown string values are left
     * alone so user customizations stay intact.
     *
     * <p>Guarded by {@link #getOffensiveMigrationMarker()} so the migration runs
     * exactly once per install.
     */
    private static void ensureOffensiveAllowedLists() {
        Path marker = getOffensiveMigrationMarker();
        if (Files.exists(marker)) return;

        Path mappingFile = getMappingFile();
        if (!Files.exists(mappingFile)) {
            // Fresh install hasn't generated the mapping yet — the first-run path
            // will create the marker itself, so just bail out here.
            return;
        }

        String content;
        try {
            content = Files.readString(mappingFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error(
                    "Failed to read instrument_auras.json for offensive-migration, skipping", e);
            return;
        }

        JsonObject root;
        try {
            root = GSON.fromJson(content, JsonObject.class);
        } catch (JsonSyntaxException e) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Invalid JSON in instrument_auras.json; skipping offensive-migration: {}", e.getMessage());
            return;
        }
        if (root == null) return;

        int upgraded = 0;
        // Snapshot keys first — we mutate the same object inside the loop.
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            keys.add(e.getKey());
        }

        for (String key : keys) {
            if (key.startsWith("_") || AuraSchema.FIELD.equals(key)) continue;
            JsonElement value = root.get(key);
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                // Object form or non-string — respect user customization.
                continue;
            }
            String positiveId = value.getAsString();
            String offensiveId = POSITIVE_TO_OFFENSIVE_AURA_ID.get(positiveId);
            if (offensiveId == null) {
                // Unknown positive aura (user-custom) — don't touch.
                continue;
            }
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
            writeAtomically(mappingFile, GSON.toJson(root));
            Files.createFile(marker);
            if (upgraded > 0) {
                EffectiveInstrumentsMod.LOGGER.info(
                        "Migrated {} instrument mapping entries to include offensive auras", upgraded);
            }
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error(
                    "Failed to write migrated instrument_auras.json (offensive pass)", e);
        }
    }

    /**
     * 1.4.4 one-shot migration: walks the mapping file and, for any instrument
     * whose default aura matches the shipped 1-to-1 assignment target (i.e. the
     * instrument was using an old duplicated id like {@code harmonic_resonance}
     * across five note-blocks), replaces the entry with the unique 1.4.4
     * assignment. Preserves custom user entries (default aura not in our
     * {@link #UNIQUE_INSTRUMENT_TO_POSITIVE} values).
     *
     * <p>Guarded by {@link #getUniqueAssignmentMarker()} so the migration runs
     * exactly once per install. Fresh installs get the marker created up-front
     * in {@link #ensureFirstRunDefaults()}.
     */
    private static void ensureUniqueAssignment() {
        Path marker = getUniqueAssignmentMarker();
        if (Files.exists(marker)) return;

        Path mappingFile = getMappingFile();
        if (!Files.exists(mappingFile)) return;

        String content;
        try {
            content = Files.readString(mappingFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error(
                    "Failed to read instrument_auras.json for unique-assignment migration, skipping", e);
            return;
        }

        JsonObject root;
        try {
            root = GSON.fromJson(content, JsonObject.class);
        } catch (JsonSyntaxException e) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Invalid JSON in instrument_auras.json; skipping unique-assignment migration: {}", e.getMessage());
            return;
        }
        if (root == null) return;

        // Set of shipped positive aura ids — these are the "known defaults" we
        // consider safe to overwrite. An entry whose default is outside this
        // set is a user customization and gets left alone.
        Set<String> shippedPositiveIds = new HashSet<>(UNIQUE_INSTRUMENT_TO_POSITIVE.values());

        int reassigned = 0;
        int added = 0;
        for (Map.Entry<String, String> target : UNIQUE_INSTRUMENT_TO_POSITIVE.entrySet()) {
            String instrumentKey = target.getKey();
            String newPositive = target.getValue();
            String newOffensive = POSITIVE_TO_OFFENSIVE_AURA_ID.get(newPositive);
            if (newOffensive == null) continue;

            JsonElement existing = root.get(instrumentKey);
            if (existing == null || existing.isJsonNull()) {
                // Missing entry — add fresh.
                addInstrumentEntry(root, instrumentKey, newPositive, newOffensive);
                added++;
                continue;
            }

            String currentDefault = extractDefaultId(existing);
            if (currentDefault == null) continue;

            // Already assigned to the canonical 1.4.4 value — nothing to do.
            if (currentDefault.equals(newPositive)) continue;

            // Custom user assignment (not a shipped id) — leave alone.
            if (!shippedPositiveIds.contains(currentDefault)) continue;

            // Reassign: current default is a shipped id that doesn't match the
            // canonical target for this instrument → it's a legacy duplicate.
            JsonObject replacement = new JsonObject();
            replacement.addProperty("default", newPositive);
            JsonArray allowed = new JsonArray();
            allowed.add(newPositive);
            allowed.add(newOffensive);
            replacement.add("allowed", allowed);
            root.add(instrumentKey, replacement);
            reassigned++;
        }

        try {
            writeAtomically(mappingFile, GSON.toJson(root));
            Files.createFile(marker);
            if (reassigned > 0 || added > 0) {
                EffectiveInstrumentsMod.LOGGER.info(
                        "1.4.4 unique-aura migration: reassigned {} duplicate mapping(s), added {} missing entry/entries",
                        reassigned, added);
            }
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error(
                    "Failed to write instrument_auras.json after unique-assignment migration", e);
        }
    }

    /**
     * Read the {@code default} field from an entry regardless of whether the
     * caller stored it as a string shorthand or as an object. Returns
     * {@code null} for malformed entries.
     */
    /**
     * Atomic replacement for {@code Files.writeString}. Writes to a sibling
     * {@code .tmp} then {@link Files#move}s with {@code ATOMIC_MOVE,
     * REPLACE_EXISTING} so a crash mid-write can't truncate the real file.
     * Falls back to non-atomic write if the filesystem rejects {@code
     * ATOMIC_MOVE} (some Windows configurations). Used by the migration
     * rewrite sites where the file holds user customization we can't afford
     * to lose.
     */
    private static void writeAtomically(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Nullable
    private static String extractDefaultId(JsonElement entry) {
        if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
            String v = entry.getAsString();
            return v.isEmpty() ? null : v;
        }
        if (entry.isJsonObject()) {
            JsonObject obj = entry.getAsJsonObject();
            if (obj.has("default") && obj.get("default").isJsonPrimitive()) {
                String v = obj.get("default").getAsString();
                return v.isEmpty() ? null : v;
            }
        }
        return null;
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

        // schemaVersion — forward-compat gate. Missing field is treated as v1.
        if (root.has(AuraSchema.FIELD)) {
            int version;
            try {
                version = root.get(AuraSchema.FIELD).getAsInt();
            } catch (Exception e) {
                EffectiveInstrumentsMod.LOGGER.error(
                        "instrument_auras.json has non-integer schemaVersion, ignoring file");
                return;
            }
            if (version > AuraSchema.CURRENT_VERSION) {
                EffectiveInstrumentsMod.LOGGER.error(
                        "instrument_auras.json has schemaVersion {} which is newer than this mod supports ({}), ignoring file",
                        version, AuraSchema.CURRENT_VERSION);
                return;
            }
        }

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) continue;
            if (AuraSchema.FIELD.equals(key)) continue;

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

        // Self-healing pass: for any mapping whose default points at a known
        // positive aura but whose allowed list is missing the offensive
        // counterpart, synthesize an augmented config in memory. If anything
        // changed AND the offensive aura is actually loaded in the registry,
        // opportunistically rewrite the file so the next reload sees clean
        // state. Failure to rewrite is non-fatal — the in-memory result is
        // authoritative.
        int synthesized = synthesizeMissingOffensiveAllowed();
        if (synthesized > 0) {
            EffectiveInstrumentsMod.LOGGER.info(
                    "Auto-added offensive aura to {} instrument mapping(s). Opportunistically rewriting file.", synthesized);
            tryRewriteMappingFile(root);
        }

        // 1.4.8: post-synthesis audit — catches duplicates introduced by user
        // edits or corrupt mapping files that weren't touched by synthesis.
        auditOffensiveUniqueness();

        EffectiveInstrumentsMod.LOGGER.info("Loaded {} instrument-aura mappings", MAPPINGS.size());
    }

    /**
     * Walks {@link #MAPPINGS}, and for each config whose default points at a
     * known positive aura but whose allowed list is missing the offensive
     * counterpart, replaces the config in place with an augmented copy.
     * Returns the count of mappings that were augmented.
     *
     * <p>Only augments when the offensive preset is actually present and
     * enabled in the registry — otherwise we'd pollute the allowed list with
     * a reference the selector would silently filter out anyway.
     */
    private static int synthesizeMissingOffensiveAllowed() {
        int count = 0;
        int offensivePresetsMissing = 0;

        // 1.4.8: batch-ified uniqueness pass. First collect every offensive id
        // already claimed by an instrument (either via a shipped pair or a
        // user-custom explicit mapping). Then iterate the gaps and, for each,
        // pick an offensive id that isn't already in `taken`. This prevents
        // the effect-based fallback from assigning the same offensive (e.g.
        // `echoes_of_decay`) to every custom regen-primary positive.
        Set<String> taken = new HashSet<>();
        for (InstrumentAuraConfig config : MAPPINGS.values()) {
            for (String id : config.allowedAuraIds()) {
                AuraRegistry.getById(id).filter(AuraPreset::isOffensive).ifPresent(p -> taken.add(id));
            }
            // Also seed `taken` with the curated pair for this instrument, even
            // if it's not in the allowed list yet — avoids the next iteration
            // grabbing it for someone else.
            String pair = POSITIVE_TO_OFFENSIVE_AURA_ID.get(config.defaultAuraId());
            if (pair != null) taken.add(pair);
        }

        for (Map.Entry<ResourceLocation, InstrumentAuraConfig> e : MAPPINGS.entrySet()) {
            InstrumentAuraConfig config = e.getValue();
            // Prefer the curated default-ID → offensive-ID pairing. If the user's
            // default is a custom positive aura (not one we ship), fall back to
            // effect-based matching so we can still suggest a sensible offensive
            // — but skip any offensive already claimed by another instrument to
            // guarantee per-instrument uniqueness.
            String offensiveId = POSITIVE_TO_OFFENSIVE_AURA_ID.get(config.defaultAuraId());
            if (offensiveId == null) {
                offensiveId = findUnusedOffensiveByEffect(config.defaultAuraId(), taken);
            }
            if (offensiveId == null) continue;
            if (config.allowedAuraIds().contains(offensiveId)) {
                taken.add(offensiveId);
                continue;
            }

            // Verify the offensive preset exists, is enabled, and supports stationary.
            var preset = AuraRegistry.getById(offensiveId);
            if (preset.isEmpty()) {
                offensivePresetsMissing++;
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Instrument '{}' default '{}' has offensive counterpart '{}', but that preset is not loaded. "
                        + "Check that config/effective_instruments/auras/{}.json exists.",
                        e.getKey(), config.defaultAuraId(), offensiveId, offensiveId);
                continue;
            }
            if (!preset.get().enabled()) {
                EffectiveInstrumentsMod.LOGGER.debug(
                        "Offensive preset '{}' is disabled — skipping synthesis for instrument '{}'",
                        offensiveId, e.getKey());
                continue;
            }
            if (!preset.get().supports(BuffTier.STATIONARY)) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "Offensive preset '{}' does not support the stationary tier — check its 'tiers' field",
                        offensiveId);
                continue;
            }

            List<String> merged = new ArrayList<>(config.allowedAuraIds());
            merged.add(offensiveId);
            e.setValue(new InstrumentAuraConfig(config.defaultAuraId(), List.copyOf(merged)));
            taken.add(offensiveId);
            count++;
            EffectiveInstrumentsMod.LOGGER.debug(
                    "Added offensive aura '{}' to instrument '{}' allow-list", offensiveId, e.getKey());
        }
        if (offensivePresetsMissing > 0) {
            EffectiveInstrumentsMod.LOGGER.error(
                    "Offensive aura synthesis found {} instrument(s) whose offensive preset file is missing. "
                    + "These instruments will show only their positive aura in the selector. "
                    + "Run /effectiveinstruments reload to retry, or delete "
                    + "config/effective_instruments/.offensive_aura_defaults_generated_v3 "
                    + "to force regeneration on next boot.", offensivePresetsMissing);
        }
        return count;
    }

    /**
     * 1.4.8: integrity check. Scans every mapping's allowed list and reports
     * offensive ids that appear on more than one instrument. Called at the
     * end of {@link #load}; warn-level so duplicates are visible in the
     * server log without hard-failing the boot. Informational for custom
     * user mappings, diagnostic for shipped ones.
     */
    private static void auditOffensiveUniqueness() {
        Map<String, List<ResourceLocation>> offensiveOwners = new HashMap<>();
        for (Map.Entry<ResourceLocation, InstrumentAuraConfig> e : MAPPINGS.entrySet()) {
            for (String auraId : e.getValue().allowedAuraIds()) {
                AuraRegistry.getById(auraId).filter(AuraPreset::isOffensive).ifPresent(p ->
                        offensiveOwners.computeIfAbsent(auraId, k -> new ArrayList<>()).add(e.getKey()));
            }
        }
        int duplicates = 0;
        for (Map.Entry<String, List<ResourceLocation>> e : offensiveOwners.entrySet()) {
            if (e.getValue().size() <= 1) continue;
            duplicates++;
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Offensive aura '{}' appears on {} instruments: {}. "
                            + "Run /effectiveinstruments reset-mappings to regenerate with unique assignment.",
                    e.getKey(), e.getValue().size(), e.getValue());
        }
        if (duplicates == 0) {
            EffectiveInstrumentsMod.LOGGER.info(
                    "Offensive-aura uniqueness audit: {} mappings, no duplicate offensive ids",
                    MAPPINGS.size());
        }
    }

    /**
     * Fallback pairing for custom positive auras that don't appear in
     * {@link #POSITIVE_TO_OFFENSIVE_AURA_ID}. Reads the positive preset's first
     * effect, looks up the canonical debuff counterpart in
     * {@link #POSITIVE_EFFECT_INVERSE}, then searches the registry for an
     * enabled, stationary-tier, offensive-polarity preset whose first effect
     * matches and that isn't already claimed by another instrument.
     *
     * <p>1.4.8: added the {@code taken} param so batched synthesis produces
     * per-instrument unique offensive ids. Falls back to any unused offensive
     * if no effect-matched candidate is available.
     *
     * <p>Keeps the search cheap: iterates all enabled presets at most twice
     * per unmapped instrument on boot.
     */
    @Nullable
    private static String findUnusedOffensiveByEffect(String positiveAuraId, Set<String> taken) {
        java.util.Optional<AuraPreset> positive = AuraRegistry.getById(positiveAuraId);
        if (positive.isEmpty() || positive.get().isOffensive()) return null;
        List<AuraPreset.EffectEntry> effects = positive.get().effects();
        if (effects.isEmpty()) return null;
        net.minecraft.resources.ResourceLocation positiveEffectId =
                net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effects.get(0).effect());
        if (positiveEffectId == null) return null;
        String inverseKey = POSITIVE_EFFECT_INVERSE.get(positiveEffectId.toString());
        if (inverseKey == null) return null;
        net.minecraft.resources.ResourceLocation inverseEffectId = new net.minecraft.resources.ResourceLocation(inverseKey);

        // Pass 1: find an effect-matched offensive that isn't already taken.
        for (AuraPreset candidate : AuraRegistry.getEnabledPresets()) {
            if (!candidate.isOffensive()) continue;
            if (!candidate.supports(BuffTier.STATIONARY)) continue;
            if (candidate.effects().isEmpty()) continue;
            if (taken.contains(candidate.id())) continue;
            net.minecraft.resources.ResourceLocation candidateEffectId =
                    net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(
                            candidate.effects().get(0).effect());
            if (inverseEffectId.equals(candidateEffectId)) {
                EffectiveInstrumentsMod.LOGGER.debug(
                        "Effect-based pairing: positive '{}' ({}) → offensive '{}' ({})",
                        positiveAuraId, positiveEffectId, candidate.id(), inverseEffectId);
                return candidate.id();
            }
        }
        // Pass 2: fall back to any unused offensive — breaks "all sharing the
        // same preset" deadlock when every effect-matched candidate is taken.
        for (AuraPreset candidate : AuraRegistry.getEnabledPresets()) {
            if (!candidate.isOffensive()) continue;
            if (!candidate.supports(BuffTier.STATIONARY)) continue;
            if (taken.contains(candidate.id())) continue;
            EffectiveInstrumentsMod.LOGGER.debug(
                    "Effect-based pairing exhausted — falling back to unused offensive '{}' for positive '{}'",
                    candidate.id(), positiveAuraId);
            return candidate.id();
        }
        return null;
    }

    /**
     * Opportunistically rewrite {@code instrument_auras.json} with the current
     * in-memory mappings. Called when {@link #synthesizeMissingOffensiveAllowed}
     * added entries so the on-disk file reflects what the runtime sees.
     * Swallows all IO errors — the in-memory map is the source of truth for
     * this session regardless.
     */
    private static void tryRewriteMappingFile(JsonObject existingRoot) {
        try {
            // Preserve non-entry top-level keys (schemaVersion, _comment, etc.).
            JsonObject merged = new JsonObject();
            for (Map.Entry<String, JsonElement> e : existingRoot.entrySet()) {
                if (e.getKey().startsWith("_") || AuraSchema.FIELD.equals(e.getKey())) {
                    merged.add(e.getKey(), e.getValue());
                }
            }
            for (Map.Entry<ResourceLocation, InstrumentAuraConfig> e : MAPPINGS.entrySet()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("default", e.getValue().defaultAuraId());
                JsonArray allowed = new JsonArray();
                for (String id : e.getValue().allowedAuraIds()) allowed.add(id);
                entry.add("allowed", allowed);
                merged.add(e.getKey().toString(), entry);
            }
            writeAtomically(getMappingFile(), GSON.toJson(merged));
            // Also drop the migration marker so the separate
            // ensureOffensiveAllowedLists() pass knows this is done.
            Path marker = getOffensiveMigrationMarker();
            if (!Files.exists(marker)) {
                try {
                    Files.createFile(marker);
                } catch (IOException ignored) {
                }
            }
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Could not rewrite instrument_auras.json after offensive synthesis: {}", e.getMessage());
        }
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
            return filterForSelector(AuraRegistry.getEnabledPresets());
        }

        InstrumentAuraConfig config = MAPPINGS.get(instrumentId);
        if (config == null) {
            return filterForSelector(AuraRegistry.getEnabledPresets());
        }

        List<AuraPreset> result = new ArrayList<>();
        for (String auraId : config.allowedAuraIds()) {
            AuraRegistry.getById(auraId)
                    .filter(AuraPreset::enabled)
                    .filter(p -> p.supports(BuffTier.STATIONARY))
                    .filter(AuraPreset::showInSelector)
                    .ifPresent(result::add);
        }
        return result;
    }

    /**
     * Keep only presets that the stationary selector UI is allowed to show:
     * stationary-tier support + explicit {@code showInSelector=true}.
     * Used when an instrument has no explicit allow-list so we still hide
     * mobile-only passive presets from the overlay.
     */
    private static List<AuraPreset> filterForSelector(List<AuraPreset> input) {
        List<AuraPreset> result = new ArrayList<>(input.size());
        for (AuraPreset preset : input) {
            if (preset.supports(BuffTier.STATIONARY) && preset.showInSelector()) {
                result.add(preset);
            }
        }
        return result;
    }

    public static int getMappingCount() {
        return MAPPINGS.size();
    }

    /**
     * Canonical polarity-pair check. Returns true iff {@code positiveId} and
     * {@code offensiveId} form a known shipped positive↔offensive pair via
     * {@link #POSITIVE_TO_OFFENSIVE_AURA_ID}. Used by the packet-handler's
     * legacy-allow-list softener to avoid accepting arbitrary opposite-
     * polarity selections.
     */
    public static boolean isKnownPolarityPair(String positiveId, String offensiveId) {
        if (positiveId == null || offensiveId == null) return false;
        return offensiveId.equals(POSITIVE_TO_OFFENSIVE_AURA_ID.get(positiveId));
    }

    /**
     * 1.4.8: admin escape hatch used by {@code /effectiveinstruments reset-mappings}.
     * Deletes the mapping JSON and every migration marker so the next call to
     * {@link #ensureDefaults} + {@link #load} regenerates everything from the
     * canonical {@link #UNIQUE_INSTRUMENT_TO_POSITIVE} table. Never touches
     * preset JSONs (those are handled by {@link AuraJsonLoader}). Returns a
     * short list of paths that were deleted for command-echo purposes.
     */
    public static List<String> resetMappings() {
        List<String> deleted = new ArrayList<>();
        for (Path p : List.of(
                getMappingFile(),
                getMarkerFile(),
                getOffensiveMigrationMarker(),
                getUniqueAssignmentMarker()
        )) {
            try {
                if (Files.deleteIfExists(p)) {
                    deleted.add(p.getFileName().toString());
                }
            } catch (IOException e) {
                EffectiveInstrumentsMod.LOGGER.warn("resetMappings: failed to delete {}: {}", p, e.getMessage());
            }
        }
        MAPPINGS.clear();
        return deleted;
    }

    private InstrumentAuraMapping() {}
}

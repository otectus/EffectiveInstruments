package com.crims.effectiveinstruments.data;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.EntityCategory;
import com.crims.effectiveinstruments.util.ConfigIO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code config/effective_instruments/entity_classification.json}.
 *
 * <p>JSON shape per spec §7:
 * <pre>
 * {
 *   "alexsmobs:gorilla": { "category": "OWN_PET", "requireTamed": true },
 *   "mca:villager":      { "delegateTo": "mca" }
 * }
 * </pre>
 *
 * <p>Lookup is by {@code EntityType<?>}. {@link #lookup(EntityType)} returns
 * {@link ClassificationOverride#EMPTY} for unmapped types so callers don't
 * need a null check.
 *
 * <p>Hot-reloadable via {@link #load()} called from
 * {@code AuraRegistry.refreshConfigDerived}.
 */
public final class EntityClassificationLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<EntityType<?>, ClassificationOverride> ENTRIES = new HashMap<>();

    private static Path getFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/entity_classification.json");
    }

    /**
     * Idempotent: writes the default JSON if the file is absent, then parses
     * + caches whatever the file currently contains. Safe to call from
     * {@code refreshConfigDerived} on every {@code /effectiveinstruments reload}.
     */
    public static void load() {
        ENTRIES.clear();
        ensureDefault();
        Path file = getFile();
        if (!Files.exists(file)) return;

        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            JsonElement root = GSON.fromJson(content, JsonElement.class);
            if (root == null || !root.isJsonObject()) return;
            JsonObject obj = root.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("_")) continue; // comments / examples
                if (!entry.getValue().isJsonObject()) continue;
                parseRow(key, entry.getValue().getAsJsonObject()).ifPresent(pair -> ENTRIES.put(pair.type(), pair.override()));
            }
            EffectiveInstrumentsMod.LOGGER.info(
                    "EI entity_classification.json loaded: {} entries", ENTRIES.size());
        } catch (IOException | JsonSyntaxException e) {
            EffectiveInstrumentsMod.LOGGER.error(
                    "Failed to load entity_classification.json — using empty overrides: {}",
                    e.getMessage());
        }
    }

    /** Returns the override for {@code type}, or {@link ClassificationOverride#EMPTY}. */
    public static ClassificationOverride lookup(EntityType<?> type) {
        return ENTRIES.getOrDefault(type, ClassificationOverride.EMPTY);
    }

    /** Read-only view used by diagnostics. */
    public static int size() { return ENTRIES.size(); }

    private static void ensureDefault() {
        Path file = getFile();
        if (Files.exists(file)) return;

        JsonObject root = new JsonObject();
        root.addProperty("_comment",
                "Per-entity-type classification overrides. Format: \"<modid>:<entity_path>\": "
                        + "{\"category\": <EntityCategory>, \"requireTamed\": <bool>, \"delegateTo\": <modid>}. "
                        + "Valid categories: MUSICIAN OWN_PET OTHER_PLAYER OTHER_PLAYER_PET VILLAGER "
                        + "IRON_GOLEM PASSIVE_MOB HOSTILE_MOB. delegateTo is reserved for cross-mod hand-off. "
                        + "Tag short-circuits this file: data/effective_instruments/tags/entity_types/{always_buff,always_debuff,ignore}.json. "
                        + "Reload with /effectiveinstruments reload.");

        // 1.6.0 Phase 5: curated defaults for the long-tail Tier-3 mods.
        // Entries are silently skipped at load if the mod isn't installed,
        // so shipping these is safe in vanilla worlds.

        // Alex's Mobs — tamed primates are companions; classify only when actually tamed.
        addEntry(root, "alexsmobs:capuchin_monkey", "OWN_PET", true);
        addEntry(root, "alexsmobs:gorilla",         "OWN_PET", true);

        // Friends & Foes — TuffGolem is the iron-golem equivalent; Glare is passive.
        addEntry(root, "friendsandfoes:tuff_golem", "IRON_GOLEM", false);
        addEntry(root, "friendsandfoes:glare",      "PASSIVE_MOB", false);

        // Twilight Forest — major bosses + custom hostiles are explicit hostiles
        // so a player's positive aura never accidentally buffs them. "Loyal Zombie"
        // is named like a pet but is hostile in the mod's lore.
        addEntry(root, "twilightforest:hydra",        "HOSTILE_MOB", false);
        addEntry(root, "twilightforest:naga",         "HOSTILE_MOB", false);
        addEntry(root, "twilightforest:lich",         "HOSTILE_MOB", false);
        addEntry(root, "twilightforest:loyal_zombie", "HOSTILE_MOB", false);

        // Cataclysm — every boss is explicitly hostile.
        addEntry(root, "cataclysm:ignis",            "HOSTILE_MOB", false);
        addEntry(root, "cataclysm:netherite_monstrosity", "HOSTILE_MOB", false);
        addEntry(root, "cataclysm:ender_guardian",   "HOSTILE_MOB", false);
        addEntry(root, "cataclysm:the_harbinger",    "HOSTILE_MOB", false);

        // Mowzie's Mobs — Umvuthana followers are passive (treated as villagers
        // by mod theme); Ferrous Wroughtnaut is hostile.
        addEntry(root, "mowziesmobs:umvuthana",       "PASSIVE_MOB", false);
        addEntry(root, "mowziesmobs:ferrous_wroughtnaut", "HOSTILE_MOB", false);
        addEntry(root, "mowziesmobs:naga",            "HOSTILE_MOB", false);

        // More Villagers — note: this mod adds PROFESSIONS to minecraft:villager,
        // not new entity types. minecraft:villager already classifies as VILLAGER
        // through the vanilla bucket, so no override entries are needed here.

        // Dungeons Mobs — every entity is hostile.
        addEntry(root, "dungeons_mobs:vindicator_armored", "HOSTILE_MOB", false);
        addEntry(root, "dungeons_mobs:pillager_royal_guard", "HOSTILE_MOB", false);

        // Born in Chaos — every entity is hostile.
        addEntry(root, "born_in_chaos_v1:soldier_undead", "HOSTILE_MOB", false);

        // Eidolon — repraised thralls are conjurer-owned; default to HOSTILE_MOB
        // here so non-conjurer-aligned players treat them as foes. Conjurers
        // get Tier-2 owner-aware classification through future EidolonOwnerProvider.
        addEntry(root, "eidolon:zombie_brute",       "HOSTILE_MOB", false);
        addEntry(root, "eidolon:wraith",             "HOSTILE_MOB", false);

        // Graveyard — Ghouling is owned by a conjurer; default treats it as
        // hostile until/unless a conjurer-owner-aware adapter overrides.
        addEntry(root, "graveyard:ghouling",         "HOSTILE_MOB", false);

        // Count only the curated entity entries — exclude the "_comment" string.
        int curatedCount = root.entrySet().size() - 1;
        try {
            ConfigIO.writeAtomically(file, GSON.toJson(root));
            EffectiveInstrumentsMod.LOGGER.info(
                    "EI created default entity_classification.json at {} with {} curated entries",
                    file, curatedCount);
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error(
                    "Failed to write default entity_classification.json: {}", e.getMessage());
        }
    }

    private static void addEntry(JsonObject root, String key, String category, boolean requireTamed) {
        JsonObject entry = new JsonObject();
        entry.addProperty("category", category);
        if (requireTamed) entry.addProperty("requireTamed", true);
        root.add(key, entry);
    }

    private static java.util.Optional<TypedEntry> parseRow(String key, JsonObject row) {
        ResourceLocation typeId = ResourceLocation.tryParse(key);
        if (typeId == null) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "entity_classification.json: invalid entity id '{}' — skipping", key);
            return java.util.Optional.empty();
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(typeId);
        if (type == null || !ForgeRegistries.ENTITY_TYPES.containsKey(typeId)) {
            // Entity type from a mod that's not loaded — silently skip.
            return java.util.Optional.empty();
        }
        EntityCategory cat = null;
        if (row.has("category")) {
            try {
                cat = EntityCategory.valueOf(row.get("category").getAsString());
            } catch (IllegalArgumentException ex) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "entity_classification.json: invalid category '{}' for {} — skipping field",
                        row.get("category").getAsString(), key);
            }
        }
        boolean requireTamed = row.has("requireTamed") && row.get("requireTamed").getAsBoolean();
        String delegateTo = row.has("delegateTo") ? row.get("delegateTo").getAsString() : null;
        if (cat == null && delegateTo == null) return java.util.Optional.empty();
        return java.util.Optional.of(new TypedEntry(type, new ClassificationOverride(cat, requireTamed, delegateTo)));
    }

    private record TypedEntry(EntityType<?> type, ClassificationOverride override) {}

    private EntityClassificationLoader() {}
}

package com.crims.effectiveinstruments.config;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraSchema;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
 * Per-instrument durability and repair metadata. Lives in
 * {@code config/effective_instruments/instrument_durability.json} and is loaded
 * alongside the aura registry (see {@code AuraRegistry.load}).
 *
 * <p>Schema (v1): each instrument ID maps to
 * {@code {"maxDurability": int, "repairMaterial": "item_id", "repairPerUnit": int}}.
 * All three fields are required per entry; missing entries fall back to the
 * {@code DURABILITY_DEFAULT_MAX} server config and no repair material.
 */
public final class InstrumentDurabilityConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Durability + repair metadata for one instrument.
     *
     * @param maxDurability total durability points before the instrument breaks
     * @param repairMaterial item ID of the material that refills via anvil, or
     *                       null if anvil-material repair is disabled for this
     *                       instrument (combine-two-damaged still works)
     * @param repairPerUnit durability restored per item consumed on the anvil
     */
    public record Entry(int maxDurability, @Nullable ResourceLocation repairMaterial, int repairPerUnit) {}

    private static final Map<ResourceLocation, Entry> ENTRIES = new HashMap<>();

    private static Path getFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/instrument_durability.json");
    }

    /**
     * Bumped to {@code _v2} in 1.4.5 to regenerate per-instrument durability
     * defaults with the 4× scale-up (200→800, 300→1200, 400→1600). Existing
     * installs will see their config file regenerated once on next boot.
     * User-edited entries in the JSON are preserved via the existence-check
     * path in {@link #ensureDefaults()} — we only regenerate when the marker
     * is absent.
     */
    private static Path getMarkerFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.instrument_durability_defaults_generated_v2");
    }

    public static void ensureDefaults() {
        Path marker = getMarkerFile();
        if (Files.exists(marker)) return;

        Path file = getFile();
        try {
            Files.createDirectories(file.getParent());

            JsonObject root = new JsonObject();
            root.addProperty(AuraSchema.FIELD, 1);
            root.addProperty("_comment",
                    "Per-instrument durability. Each entry: {maxDurability, repairMaterial, repairPerUnit}. "
                            + "Remove an entry to fall back to the server-config defaultMax; set repairMaterial to \"\" to disable anvil-material repair. "
                            + "Reload with /effectiveinstruments reload.");

            JsonObject instruments = new JsonObject();

            // 1.4.5: per-instrument base durabilities scaled 4× (200/300/400 →
            // 800/1200/1600). Repair-per-unit scaled to match so a single repair
            // unit is still worth ~1/5 of max.

            // Genshin Instruments — medium tier (1200).
            addEntry(instruments, "genshinstrument:windsong_lyre", 1200, "minecraft:phantom_membrane", 240);
            addEntry(instruments, "genshinstrument:vintage_lyre", 1200, "minecraft:gold_ingot", 240);
            addEntry(instruments, "genshinstrument:floral_zither", 1200, "minecraft:leather", 240);
            addEntry(instruments, "genshinstrument:glorious_drum", 1600, "minecraft:leather", 320);
            addEntry(instruments, "genshinstrument:nightwind_horn", 1200, "minecraft:ender_pearl", 240);
            addEntry(instruments, "genshinstrument:ukulele", 800, "minecraft:string", 160);
            addEntry(instruments, "genshinstrument:djem_djem_drum", 1600, "minecraft:leather", 320);

            // Even More Instruments — mixed tier.
            addEntry(instruments, "evenmoreinstruments:guitar", 1200, "minecraft:string", 240);
            addEntry(instruments, "evenmoreinstruments:keyboard", 1600, "minecraft:redstone", 320);
            addEntry(instruments, "evenmoreinstruments:koto", 1200, "minecraft:string", 240);
            addEntry(instruments, "evenmoreinstruments:pipa", 1200, "minecraft:string", 240);
            addEntry(instruments, "evenmoreinstruments:saxophone", 1200, "minecraft:copper_ingot", 240);
            addEntry(instruments, "evenmoreinstruments:shamisen", 1200, "minecraft:string", 240);
            addEntry(instruments, "evenmoreinstruments:trombone", 1600, "minecraft:copper_ingot", 320);
            addEntry(instruments, "evenmoreinstruments:violin", 1200, "minecraft:string", 240);

            // EMI note-block variants — sharing durability with their instrument peers.
            addEntry(instruments, "evenmoreinstruments:harp_note_block_instrument", 1200, "minecraft:string", 240);
            addEntry(instruments, "evenmoreinstruments:bass_note_block_instrument", 1200, "minecraft:string", 240);
            addEntry(instruments, "evenmoreinstruments:basedrum_note_block_instrument", 1600, "minecraft:leather", 320);
            addEntry(instruments, "evenmoreinstruments:snare_note_block_instrument", 1600, "minecraft:leather", 320);
            addEntry(instruments, "evenmoreinstruments:hat_note_block_instrument", 800, "minecraft:leather", 160);
            addEntry(instruments, "evenmoreinstruments:bell_note_block_instrument", 1200, "minecraft:gold_ingot", 240);
            addEntry(instruments, "evenmoreinstruments:chime_note_block_instrument", 800, "minecraft:iron_ingot", 160);
            addEntry(instruments, "evenmoreinstruments:flute_note_block_instrument", 800, "minecraft:feather", 160);
            addEntry(instruments, "evenmoreinstruments:guitar_note_block_instrument", 1200, "minecraft:string", 240);
            addEntry(instruments, "evenmoreinstruments:xylophone_note_block_instrument", 1200, "minecraft:copper_ingot", 240);
            addEntry(instruments, "evenmoreinstruments:iron_xylophone_note_block_instrument", 1200, "minecraft:iron_ingot", 240);
            addEntry(instruments, "evenmoreinstruments:cow_bell_note_block_instrument", 800, "minecraft:copper_ingot", 160);
            addEntry(instruments, "evenmoreinstruments:didgeridoo_note_block_instrument", 1200, "minecraft:bamboo", 240);
            addEntry(instruments, "evenmoreinstruments:bit_note_block_instrument", 1200, "minecraft:redstone", 240);
            addEntry(instruments, "evenmoreinstruments:banjo_note_block_instrument", 1200, "minecraft:string", 240);
            addEntry(instruments, "evenmoreinstruments:pling_note_block_instrument", 1200, "minecraft:redstone", 240);

            // Immersive Melodies — mobile-tier instruments (lighter side).
            addEntry(instruments, "immersive_melodies:flute", 800, "minecraft:feather", 160);
            addEntry(instruments, "immersive_melodies:lute", 1200, "minecraft:string", 240);
            addEntry(instruments, "immersive_melodies:piano", 1600, "minecraft:redstone", 320);
            addEntry(instruments, "immersive_melodies:vielle", 1200, "minecraft:string", 240);
            addEntry(instruments, "immersive_melodies:didgeridoo", 1200, "minecraft:bamboo", 240);
            addEntry(instruments, "immersive_melodies:bagpipe", 1600, "minecraft:leather", 320);
            addEntry(instruments, "immersive_melodies:trumpet", 1600, "minecraft:copper_ingot", 320);
            addEntry(instruments, "immersive_melodies:tiny_drum", 800, "minecraft:leather", 160);
            addEntry(instruments, "immersive_melodies:triangle", 800, "minecraft:iron_ingot", 160);
            addEntry(instruments, "immersive_melodies:handpan", 1600, "minecraft:iron_ingot", 320);
            addEntry(instruments, "immersive_melodies:ender_bass", 1600, "minecraft:ender_pearl", 320);

            root.add("instruments", instruments);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.createFile(marker);

            EffectiveInstrumentsMod.LOGGER.info("Generated default instrument durability mappings");
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to generate default instrument durability mappings", e);
        }
    }

    private static void addEntry(JsonObject instruments, String itemId, int max, String material, int perUnit) {
        JsonObject obj = new JsonObject();
        obj.addProperty("maxDurability", max);
        obj.addProperty("repairMaterial", material);
        obj.addProperty("repairPerUnit", perUnit);
        instruments.add(itemId, obj);
    }

    public static void load() {
        ENTRIES.clear();

        Path file = getFile();
        if (!Files.exists(file)) {
            EffectiveInstrumentsMod.LOGGER.debug("No instrument_durability.json found");
            return;
        }

        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to read instrument_durability.json", e);
            return;
        }

        JsonObject root;
        try {
            root = GSON.fromJson(content, JsonObject.class);
        } catch (JsonSyntaxException e) {
            EffectiveInstrumentsMod.LOGGER.error(
                    "Invalid JSON in instrument_durability.json: {}", e.getMessage());
            return;
        }
        if (root == null) return;

        JsonElement instrumentsElem = root.get("instruments");
        if (instrumentsElem == null || !instrumentsElem.isJsonObject()) {
            EffectiveInstrumentsMod.LOGGER.warn("instrument_durability.json missing 'instruments' object");
            return;
        }

        int loaded = 0;
        for (Map.Entry<String, JsonElement> e : instrumentsElem.getAsJsonObject().entrySet()) {
            String key = e.getKey();
            if (!ResourceLocation.isValidResourceLocation(key)) {
                EffectiveInstrumentsMod.LOGGER.warn("Invalid instrument ID '{}' in durability config", key);
                continue;
            }
            if (!e.getValue().isJsonObject()) continue;
            JsonObject obj = e.getValue().getAsJsonObject();

            int max = obj.has("maxDurability")
                    ? obj.get("maxDurability").getAsInt()
                    : EIServerConfig.DURABILITY_DEFAULT_MAX.get();
            max = Math.max(1, max);

            ResourceLocation material = null;
            if (obj.has("repairMaterial") && obj.get("repairMaterial").isJsonPrimitive()) {
                String raw = obj.get("repairMaterial").getAsString();
                if (!raw.isEmpty() && ResourceLocation.isValidResourceLocation(raw)) {
                    ResourceLocation candidate = new ResourceLocation(raw);
                    Item item = ForgeRegistries.ITEMS.getValue(candidate);
                    if (item == null) {
                        EffectiveInstrumentsMod.LOGGER.warn(
                                "Durability config for '{}' references unknown repair material '{}'", key, raw);
                    } else {
                        material = candidate;
                    }
                }
            }

            int perUnit = obj.has("repairPerUnit")
                    ? Math.max(1, obj.get("repairPerUnit").getAsInt())
                    : Math.max(1, max / 5);

            ENTRIES.put(new ResourceLocation(key), new Entry(max, material, perUnit));
            loaded++;
        }

        EffectiveInstrumentsMod.LOGGER.info("Loaded {} instrument durability entries", loaded);
    }

    @Nullable
    public static Entry get(ResourceLocation itemId) {
        return ENTRIES.get(itemId);
    }

    public static int getEntryCount() {
        return ENTRIES.size();
    }

    private InstrumentDurabilityConfig() {}
}

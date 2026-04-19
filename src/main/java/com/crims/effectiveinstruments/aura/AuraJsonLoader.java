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

    /**
     * Stationary positive defaults marker. Bumped to {@code _v2} in 1.4.4 to force
     * regeneration — the positive-preset table grew from 15 to 31 entries so every
     * shipped instrument has its own unique positive aura.
     */
    private static Path getMarkerFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.defaults_generated_v2");
    }

    /**
     * Distinct marker for the mobile-tier preset defaults added in 1.3.0. Using a
     * separate marker from {@link #getMarkerFile()} is the non-negotiable migration
     * detail — upgraded installs already have the stationary marker, so reusing it
     * would silently skip the new mobile defaults.
     *
     * <p>Bumped to {@code _v2} in 1.4.2 so 1.3.x/1.4.x installs regenerate mobile
     * JSONs with the new {@code icon}/{@code iconSelected} and
     * {@code showInSelector=true} fields.
     */
    private static Path getMobileMarkerFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.mobile_aura_defaults_generated_v2");
    }

    /**
     * Distinct marker for the offensive (negative-polarity) preset defaults added in 1.4.0.
     * Same reasoning as the mobile marker: pre-1.4.0 installs already have the stationary
     * marker present, so reusing it would silently skip the new offensive defaults.
     *
     * <p>Bumped to {@code _v3} in 1.4.4 — the offensive default table doubled in size to
     * assign a unique offensive counterpart to every shipped instrument (31 stationary
     * entries, up from 15). Existing installs regenerate their offensive JSONs once.
     */
    private static Path getOffensiveMarkerFile() {
        return FMLPaths.CONFIGDIR.get().resolve("effective_instruments/.offensive_aura_defaults_generated_v3");
    }

    /**
     * One entry in the static offensive-defaults table. A single source of truth
     * for both the first-run write and the load-time presence-check — if either
     * of those two call sites diverges, users end up in inconsistent states.
     */
    private record OffensiveDefault(
            String id,
            String displayName,
            String description,
            String color,
            int durationTicks,
            int sortOrder,
            boolean stationary,
            String[][] effects
    ) {}

    /**
     * The full table of offensive presets shipped with 1.4.1. Order matches the
     * original expansion doc §2 table. Both {@link #ensureOffensiveDefaults()}
     * and {@link #healMissingOffensivePresets()} iterate this — never duplicate.
     */
    private static final List<OffensiveDefault> OFFENSIVE_DEFAULTS = List.of(
            // Stationary (Genshin + EMI). 31 entries — one unique offensive per
            // shipped instrument. Every effects+amplifier tuple in this table is
            // distinct (verified by AuraRegistry's uniqueness guard). 1.4.4
            // fixed the earth_tremor/fathomless_pull tuple duplicate from 1.4.x.
            new OffensiveDefault("zephyrs_wrath", "Zephyr's Wrath",
                    "A howling headwind slows and weakens nearby foes", "2E4A5E", 80, 300, true,
                    new String[][]{{"minecraft:slowness", "1"}, {"minecraft:weakness", "0"}}),
            new OffensiveDefault("echoes_of_decay", "Echoes of Decay",
                    "Funerary notes wither and tire nearby foes", "4A3A4A", 100, 301, true,
                    new String[][]{{"minecraft:wither", "0"}, {"minecraft:mining_fatigue", "0"}}),
            new OffensiveDefault("withering_bloom", "Withering Bloom",
                    "Corrupted petals poison and sap nearby foes", "6B2E6B", 100, 302, true,
                    new String[][]{{"minecraft:poison", "1"}, {"minecraft:weakness", "0"}}),
            new OffensiveDefault("battle_fanfare", "Battle Fanfare",
                    "A concussive war beat weakens and slows nearby foes", "7A1F1F", 80, 303, true,
                    new String[][]{{"minecraft:weakness", "1"}, {"minecraft:slowness", "0"}}),
            new OffensiveDefault("ebon_dirge", "Ebon Dirge",
                    "A discordant funeral horn blinds and sickens nearby foes", "2B1F4A", 120, 304, true,
                    new String[][]{{"minecraft:blindness", "0"}, {"minecraft:nausea", "0"}}),
            new OffensiveDefault("mischief_melody", "Mischief Melody",
                    "A jinxing tune blinds and poisons nearby foes", "7A6A1A", 80, 305, true,
                    new String[][]{{"minecraft:blindness", "0"}, {"minecraft:poison", "0"}}),
            new OffensiveDefault("earth_tremor", "Earth Tremor",
                    "A rumbling earth-beat slows and fatigues nearby foes", "5A3A1F", 80, 306, true,
                    new String[][]{{"minecraft:slowness", "0"}, {"minecraft:mining_fatigue", "1"}}),
            new OffensiveDefault("storm_drifter", "Storm Drifter",
                    "An angry riff wears down and slows nearby foes", "3A2A5A", 80, 307, true,
                    new String[][]{{"minecraft:weakness", "0"}, {"minecraft:slowness", "0"}}),
            new OffensiveDefault("discordant_chord", "Discordant Chord",
                    "An unsettling chord slows and saps the stamina of nearby foes", "3A3A4A", 100, 308, true,
                    new String[][]{{"minecraft:slowness", "0"}, {"minecraft:hunger", "0"}}),
            new OffensiveDefault("fathomless_pull", "Fathomless Pull",
                    "Deep resonance drags nearby foes into dizzying currents", "1F3A5A", 120, 309, true,
                    new String[][]{{"minecraft:nausea", "1"}, {"minecraft:slowness", "0"}}),
            new OffensiveDefault("winds_of_war", "Winds of War",
                    "A harsh war-chant withers nearby foes", "5A1F1F", 100, 310, true,
                    new String[][]{{"minecraft:wither", "0"}}),
            new OffensiveDefault("bleeding_note", "Bleeding Note",
                    "A piercing jazz note disorients nearby foes", "5A4A1F", 60, 311, true,
                    new String[][]{{"minecraft:weakness", "0"}, {"minecraft:levitation", "0"}}),
            new OffensiveDefault("spectral_wail", "Spectral Wail",
                    "A ghostly cry nauseates nearby foes", "3A4A4A", 80, 312, true,
                    new String[][]{{"minecraft:nausea", "2"}}),
            new OffensiveDefault("obsidian_blast", "Obsidian Blast",
                    "A concussive brass blast lifts and slows nearby foes", "1A1A1A", 100, 313, true,
                    new String[][]{{"minecraft:levitation", "0"}, {"minecraft:slowness", "1"}}),
            new OffensiveDefault("dirge_of_grief", "Dirge of Grief",
                    "A sorrowful strain withers and sickens nearby foes", "4A2A3A", 120, 314, true,
                    new String[][]{{"minecraft:wither", "0"}, {"minecraft:nausea", "0"}}),
            // --- 1.4.4: 16 new offensive stationary presets (one per EMI note-block) ---
            new OffensiveDefault("skyward_blight", "Skyward Blight",
                    "An ill wind dulls the wits of nearby foes", "3A4A6E", 80, 315, true,
                    new String[][]{{"minecraft:nausea", "0"}, {"minecraft:slowness", "0"}}),
            new OffensiveDefault("rumbling_curse", "Rumbling Curse",
                    "A low groan weakens and starves nearby foes", "3A2A1F", 100, 316, true,
                    new String[][]{{"minecraft:hunger", "0"}, {"minecraft:weakness", "0"}}),
            new OffensiveDefault("thunderous_dirge", "Thunderous Dirge",
                    "A pounding salvo drains the arms of nearby foes", "4A2A2A", 100, 317, true,
                    new String[][]{{"minecraft:mining_fatigue", "1"}, {"minecraft:weakness", "0"}}),
            new OffensiveDefault("drumline_blight", "Drumline Blight",
                    "A chaotic cadence lifts and staggers nearby foes", "5A2A4A", 80, 318, true,
                    new String[][]{{"minecraft:levitation", "1"}, {"minecraft:slowness", "1"}}),
            new OffensiveDefault("artisan_curse", "Artisan's Curse",
                    "A shrill hat-strike disorients and tires nearby foes", "3A3A1F", 80, 319, true,
                    new String[][]{{"minecraft:mining_fatigue", "0"}, {"minecraft:nausea", "0"}}),
            new OffensiveDefault("tolling_entropy", "Tolling Entropy",
                    "A funeral bell withers and drains nearby foes", "4A3A2A", 120, 320, true,
                    new String[][]{{"minecraft:weakness", "1"}, {"minecraft:wither", "1"}}),
            new OffensiveDefault("starlit_malice", "Starlit Malice",
                    "A cold chime blinds and sickens nearby foes", "2A2A4A", 100, 321, true,
                    new String[][]{{"minecraft:blindness", "1"}, {"minecraft:nausea", "0"}}),
            new OffensiveDefault("fleetfoot_fall", "Fleetfoot Fall",
                    "A swooning trill unsteadies and lifts nearby foes", "3A3A5A", 80, 322, true,
                    new String[][]{{"minecraft:levitation", "0"}, {"minecraft:slowness", "0"}}),
            new OffensiveDefault("troubadour_dirge", "Troubadour's Dirge",
                    "A mournful strum blinds and weakens nearby foes", "4A2A4A", 100, 323, true,
                    new String[][]{{"minecraft:blindness", "0"}, {"minecraft:weakness", "0"}}),
            new OffensiveDefault("craftwork_rot", "Craftwork Rot",
                    "A brittle xylophone run saps hunger and strength from nearby foes", "3A3A2A", 100, 324, true,
                    new String[][]{{"minecraft:hunger", "0"}, {"minecraft:mining_fatigue", "0"}}),
            new OffensiveDefault("ironwright_curse", "Ironwright's Curse",
                    "A leaden toll dulls arms and hands of nearby foes", "2A2A2A", 100, 325, true,
                    new String[][]{{"minecraft:mining_fatigue", "0"}, {"minecraft:weakness", "0"}}),
            new OffensiveDefault("pasture_rot", "Pasture Rot",
                    "A sour cow-bell jangles and sickens nearby foes", "4A3A1F", 120, 326, true,
                    new String[][]{{"minecraft:nausea", "1"}, {"minecraft:wither", "0"}}),
            new OffensiveDefault("hearthshade_dirge", "Hearthshade Dirge",
                    "A hollow drone blinds and withers nearby foes", "2A1A1A", 120, 327, true,
                    new String[][]{{"minecraft:blindness", "0"}, {"minecraft:wither", "0"}}),
            new OffensiveDefault("pixel_rot", "Pixel Rot",
                    "A jagged bit-run tires and slows nearby foes", "3A3A3A", 80, 328, true,
                    new String[][]{{"minecraft:mining_fatigue", "1"}, {"minecraft:slowness", "1"}}),
            new OffensiveDefault("wayfinders_lament", "Wayfinder's Lament",
                    "A sour banjo-twang disorients and slows nearby foes", "4A3A1A", 100, 329, true,
                    new String[][]{{"minecraft:nausea", "1"}, {"minecraft:slowness", "1"}}),
            new OffensiveDefault("bellwether_rot", "Bellwether Rot",
                    "A tolling pling blinds and dulls the arms of nearby foes", "3A2A3A", 100, 330, true,
                    new String[][]{{"minecraft:blindness", "0"}, {"minecraft:mining_fatigue", "0"}}),

            // Mobile (Immersive Melodies) — each single-effect
            new OffensiveDefault("gale_shock_mobile", "Gale Shock",
                    "A sharp whistle stumbles nearby foes", "2E4A5E", 60, 400, false,
                    new String[][]{{"minecraft:slowness", "0"}}),
            new OffensiveDefault("calamitys_chant_mobile", "Calamity's Chant",
                    "A creeping curse poisons nearby foes", "4A3A4A", 60, 401, false,
                    new String[][]{{"minecraft:poison", "0"}}),
            new OffensiveDefault("iron_hammer_mobile", "Iron Hammer",
                    "A sharp percussive hit dulls nearby foes", "3A3A4A", 60, 402, false,
                    new String[][]{{"minecraft:weakness", "0"}}),
            new OffensiveDefault("winters_chill_mobile", "Winter's Chill",
                    "A cold warble chills the feet of nearby foes", "1F3A5A", 60, 403, false,
                    new String[][]{{"minecraft:slowness", "0"}}),
            new OffensiveDefault("stone_ward_mobile", "Stone Ward",
                    "A low drone saps the digging strength of nearby foes", "5A3A1F", 60, 404, false,
                    new String[][]{{"minecraft:mining_fatigue", "0"}}),
            new OffensiveDefault("battle_march_mobile", "Battle March",
                    "A discordant drone imposes weariness on nearby foes", "7A1F1F", 60, 405, false,
                    new String[][]{{"minecraft:weakness", "0"}}),
            new OffensiveDefault("war_cry_mobile", "War Cry",
                    "A brassy blare demoralizes nearby foes", "5A1F1F", 40, 406, false,
                    new String[][]{{"minecraft:weakness", "0"}}),
            new OffensiveDefault("war_drum_mobile", "War Drum",
                    "A steady beat tires nearby foes", "7A6A1A", 60, 407, false,
                    new String[][]{{"minecraft:slowness", "0"}}),
            new OffensiveDefault("echoing_chill_mobile", "Echoing Chill",
                    "A soft tinkle unnerves nearby foes", "2B1F4A", 100, 408, false,
                    new String[][]{{"minecraft:blindness", "0"}}),
            new OffensiveDefault("abyssal_rattle_mobile", "Abyssal Rattle",
                    "A sickening melody nauseates nearby foes", "3A4A4A", 100, 409, false,
                    new String[][]{{"minecraft:nausea", "1"}}),
            new OffensiveDefault("soul_tremor_mobile", "Soul Tremor",
                    "A dark pulse withers nearby foes", "1A1A2A", 80, 410, false,
                    new String[][]{{"minecraft:wither", "0"}})
    );

    // --- Default generation ---

    public static void ensureDefaults() {
        ensureStationaryDefaults();
        ensureMobileDefaults();
        ensureOffensiveDefaults();
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

            // --- 1.4.4: 16 new positive presets so every shipped instrument has
            // its own unique positive aura. Each preset below has an effects+amp
            // tuple distinct from every other preset in this loader (verified by
            // AuraRegistry's uniqueness guard at load time).

            writeDefaultJson(aurasDir, "skyward_zephyr", "Skyward Zephyr",
                    "A bright noteplume quickens nearby allies and sharpens their luck", "B4D8FF", 160, 15,
                    new String[][]{{"minecraft:luck", "0"}, {"minecraft:speed", "0"}},
                    "effectiveinstruments:textures/gui/aura_skyward_zephyr.png",
                    "effectiveinstruments:textures/gui/aura_skyward_zephyr_selected.png");

            writeDefaultJson(aurasDir, "rumbling_anthem", "Rumbling Anthem",
                    "A deep bassline emboldens and feeds nearby allies", "8B4513", 160, 16,
                    new String[][]{{"minecraft:saturation", "0"}, {"minecraft:strength", "0"}},
                    "effectiveinstruments:textures/gui/aura_rumbling_anthem.png",
                    "effectiveinstruments:textures/gui/aura_rumbling_anthem_selected.png");

            writeDefaultJson(aurasDir, "thunderous_cadence", "Thunderous Cadence",
                    "A pounding drum fuels the arms and hands of nearby allies", "CC3300", 160, 17,
                    new String[][]{{"minecraft:haste", "0"}, {"minecraft:strength", "0"}},
                    "effectiveinstruments:textures/gui/aura_thunderous_cadence.png",
                    "effectiveinstruments:textures/gui/aura_thunderous_cadence_selected.png");

            writeDefaultJson(aurasDir, "drumline_vigor", "Drumline Vigor",
                    "A sharp snare cadence sets a higher gear for nearby allies", "D2B48C", 160, 18,
                    new String[][]{{"minecraft:speed", "1"}, {"minecraft:jump_boost", "1"}},
                    "effectiveinstruments:textures/gui/aura_drumline_vigor.png",
                    "effectiveinstruments:textures/gui/aura_drumline_vigor_selected.png");

            writeDefaultJson(aurasDir, "artisan_tempo", "Artisan Tempo",
                    "A crisp hi-hat quickens the hands and fortune of nearby allies", "E0E0E0", 160, 19,
                    new String[][]{{"minecraft:haste", "0"}, {"minecraft:luck", "0"}},
                    "effectiveinstruments:textures/gui/aura_artisan_tempo.png",
                    "effectiveinstruments:textures/gui/aura_artisan_tempo_selected.png");

            writeDefaultJson(aurasDir, "chiming_revival", "Chiming Revival",
                    "Bright bells mend and win the goodwill of villagers for nearby allies", "FFE066", 200, 20,
                    new String[][]{{"minecraft:regeneration", "0"}, {"minecraft:hero_of_the_village", "0"}},
                    "effectiveinstruments:textures/gui/aura_chiming_revival.png",
                    "effectiveinstruments:textures/gui/aura_chiming_revival_selected.png");

            writeDefaultJson(aurasDir, "starlit_grace", "Starlit Grace",
                    "A shimmering chime sharpens night sight and aquatic grace for nearby allies", "AACCFF", 260, 21,
                    new String[][]{{"minecraft:night_vision", "0"}, {"minecraft:dolphins_grace", "0"}},
                    "effectiveinstruments:textures/gui/aura_starlit_grace.png",
                    "effectiveinstruments:textures/gui/aura_starlit_grace_selected.png");

            writeDefaultJson(aurasDir, "fleetfoot_lilt", "Fleetfoot Lilt",
                    "A dancing flute-line lightens the step and speeds nearby allies", "9FE5B3", 160, 22,
                    new String[][]{{"minecraft:speed", "0"}, {"minecraft:slow_falling", "0"}},
                    "effectiveinstruments:textures/gui/aura_fleetfoot_lilt.png",
                    "effectiveinstruments:textures/gui/aura_fleetfoot_lilt_selected.png");

            writeDefaultJson(aurasDir, "troubadour_march", "Troubadour's March",
                    "A bold ballad emboldens nearby allies and sharpens their night sight", "5A3A6E", 200, 23,
                    new String[][]{{"minecraft:strength", "0"}, {"minecraft:night_vision", "0"}},
                    "effectiveinstruments:textures/gui/aura_troubadour_march.png",
                    "effectiveinstruments:textures/gui/aura_troubadour_march_selected.png");

            writeDefaultJson(aurasDir, "craftwork_rondo", "Craftwork Rondo",
                    "A bright xylophone run quickens the hands and feeds nearby allies", "F0D67A", 160, 24,
                    new String[][]{{"minecraft:haste", "0"}, {"minecraft:saturation", "0"}},
                    "effectiveinstruments:textures/gui/aura_craftwork_rondo.png",
                    "effectiveinstruments:textures/gui/aura_craftwork_rondo_selected.png");

            writeDefaultJson(aurasDir, "ironwright_anthem", "Ironwright Anthem",
                    "A tempered xylophone toll braces and quickens nearby allies", "B0B8C0", 160, 25,
                    new String[][]{{"minecraft:resistance", "0"}, {"minecraft:haste", "0"}},
                    "effectiveinstruments:textures/gui/aura_ironwright_anthem.png",
                    "effectiveinstruments:textures/gui/aura_ironwright_anthem_selected.png");

            writeDefaultJson(aurasDir, "pasture_serenade", "Pasture Serenade",
                    "A warm cow-bell jingle mends and brings fortune to nearby allies", "D9A43C", 200, 26,
                    new String[][]{{"minecraft:luck", "0"}, {"minecraft:regeneration", "0"}},
                    "effectiveinstruments:textures/gui/aura_pasture_serenade.png",
                    "effectiveinstruments:textures/gui/aura_pasture_serenade_selected.png");

            writeDefaultJson(aurasDir, "hearthlight_drone", "Hearthlight Drone",
                    "A warming drone sharpens night sight and shields from flame for nearby allies", "FF8844", 260, 27,
                    new String[][]{{"minecraft:night_vision", "0"}, {"minecraft:fire_resistance", "0"}},
                    "effectiveinstruments:textures/gui/aura_hearthlight_drone.png",
                    "effectiveinstruments:textures/gui/aura_hearthlight_drone_selected.png");

            writeDefaultJson(aurasDir, "pixel_pulse", "Pixel Pulse",
                    "A staccato bit-run quickens the hands and springs the step of nearby allies", "66DDAA", 160, 28,
                    new String[][]{{"minecraft:haste", "1"}, {"minecraft:jump_boost", "1"}},
                    "effectiveinstruments:textures/gui/aura_pixel_pulse.png",
                    "effectiveinstruments:textures/gui/aura_pixel_pulse_selected.png");

            writeDefaultJson(aurasDir, "wayfinders_reel", "Wayfinder's Reel",
                    "A lively banjo reel quickens nearby allies and brings fortune", "CC9933", 160, 29,
                    new String[][]{{"minecraft:luck", "1"}, {"minecraft:speed", "1"}},
                    "effectiveinstruments:textures/gui/aura_wayfinders_reel.png",
                    "effectiveinstruments:textures/gui/aura_wayfinders_reel_selected.png");

            writeDefaultJson(aurasDir, "bellwether_toll", "Bellwether Toll",
                    "A resonant pling sharpens night sight and quickens the hands of nearby allies", "8F8FFF", 200, 30,
                    new String[][]{{"minecraft:haste", "0"}, {"minecraft:night_vision", "0"}},
                    "effectiveinstruments:textures/gui/aura_bellwether_toll.png",
                    "effectiveinstruments:textures/gui/aura_bellwether_toll_selected.png");

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
     * Generate the offensive-polarity defaults added in 1.4.0. Each preset carries
     * {@code "polarity": "negative"} and uses the same sortOrder bands as the
     * positive tier (300-series for stationary, 400-series for mobile) so they
     * land together but below the existing presets.
     */
    private static void ensureOffensiveDefaults() {
        Path marker = getOffensiveMarkerFile();
        if (Files.exists(marker)) return;

        Path aurasDir = getAurasDir();
        try {
            Files.createDirectories(aurasDir);
            for (OffensiveDefault preset : OFFENSIVE_DEFAULTS) {
                writeOffensiveDefault(aurasDir, preset);
            }
            Files.createFile(marker);
            EffectiveInstrumentsMod.LOGGER.info("Generated offensive default aura presets");
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.error("Failed to generate offensive default aura presets", e);
        }
    }

    /**
     * Load-time self-healing: after the initial preset list is assembled,
     * verify every entry from {@link #OFFENSIVE_DEFAULTS} has a corresponding
     * JSON file on disk. If any are missing (partial initial-write, user
     * deleted by accident, etc.), regenerate them and return the list of
     * IDs that needed healing. Callers should re-run {@link #loadAll()} to
     * pick up the regenerated files.
     *
     * <p>This runs regardless of the marker file — the marker is an
     * optimisation, not a correctness gate.
     */
    public static List<String> healMissingOffensivePresets() {
        Path aurasDir = getAurasDir();
        List<String> healed = new ArrayList<>();
        try {
            if (!Files.isDirectory(aurasDir)) {
                Files.createDirectories(aurasDir);
            }
            for (OffensiveDefault preset : OFFENSIVE_DEFAULTS) {
                Path file = aurasDir.resolve(preset.id() + ".json");
                if (Files.exists(file)) continue;
                try {
                    writeOffensiveDefault(aurasDir, preset);
                    healed.add(preset.id());
                } catch (IOException e) {
                    EffectiveInstrumentsMod.LOGGER.warn(
                            "Failed to heal missing offensive preset '{}': {}", preset.id(), e.getMessage());
                }
            }
        } catch (IOException e) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Failed to open auras directory for presence-check: {}", e.getMessage());
        }
        if (!healed.isEmpty()) {
            EffectiveInstrumentsMod.LOGGER.info(
                    "Regenerated {} missing offensive preset file(s): {}", healed.size(), healed);
        }
        return healed;
    }

    private static void writeOffensiveDefault(Path aurasDir, OffensiveDefault def) throws IOException {
        if (def.stationary()) {
            writeOffensiveStationaryDefault(aurasDir, def.id(), def.displayName(), def.description(),
                    def.color(), def.durationTicks(), def.sortOrder(), def.effects());
        } else {
            // Mobile defaults are single-effect; flatten the 2D array back to scalar args.
            String effectId = def.effects()[0][0];
            int amplifier = Integer.parseInt(def.effects()[0][1]);
            writeOffensiveMobileDefault(aurasDir, def.id(), def.displayName(), def.description(),
                    def.color(), def.durationTicks(), def.sortOrder(), effectId, amplifier);
        }
    }

    /**
     * Helper for a multi-effect, stationary offensive preset. Emits
     * {@code "polarity": "negative"}, stationary-only tiers, and
     * {@code showInSelector=true}.
     */
    private static void writeOffensiveStationaryDefault(
            Path dir, String id, String displayName, String description,
            String color, int durationTicks, int sortOrder,
            String[][] effects
    ) throws IOException {
        Path file = dir.resolve(id + ".json");
        // Preserve user edits across marker bumps (same contract as writeDefaultJson).
        if (Files.exists(file)) return;
        JsonObject root = baseOffensiveJson(displayName, description, color, durationTicks, sortOrder);
        JsonArray tiers = new JsonArray();
        tiers.add("stationary");
        root.add("tiers", tiers);
        root.addProperty("showInSelector", true);
        appendEffects(root, effects);
        appendOffensiveIcons(root, id);
        Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    /**
     * Helper for a single-effect, mobile-only offensive preset. Emits
     * {@code "polarity": "negative"}, mobile-only tiers, and
     * {@code showInSelector=false}.
     */
    private static void writeOffensiveMobileDefault(
            Path dir, String id, String displayName, String description,
            String color, int durationTicks, int sortOrder,
            String effectId, int amplifier
    ) throws IOException {
        Path file = dir.resolve(id + ".json");
        if (Files.exists(file)) return;
        JsonObject root = baseOffensiveJson(displayName, description, color, durationTicks, sortOrder);
        JsonArray tiers = new JsonArray();
        tiers.add("mobile");
        root.add("tiers", tiers);
        root.addProperty("showInSelector", false);
        appendEffects(root, new String[][]{{effectId, Integer.toString(amplifier)}});
        appendOffensiveIcons(root, id);
        Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    /**
     * Emit {@code icon}/{@code iconSelected} fields pointing at the procedurally-
     * generated offensive PNGs (see {@code tools/gen_offensive_icons.py}). Paths
     * mirror the positive-preset convention: {@code aura_<id>.png} and
     * {@code aura_<id>_selected.png} under
     * {@code assets/effectiveinstruments/textures/gui/}.
     */
    private static void appendOffensiveIcons(JsonObject root, String id) {
        root.addProperty("icon", "effectiveinstruments:textures/gui/aura_" + id + ".png");
        root.addProperty("iconSelected", "effectiveinstruments:textures/gui/aura_" + id + "_selected.png");
    }

    private static JsonObject baseOffensiveJson(
            String displayName, String description, String color, int durationTicks, int sortOrder
    ) {
        JsonObject root = new JsonObject();
        root.addProperty(AuraSchema.FIELD, AuraSchema.CURRENT_VERSION);
        root.addProperty("displayName", displayName);
        root.addProperty("description", description);
        root.addProperty("color", color);
        root.addProperty("enabled", true);
        root.addProperty("durationTicks", durationTicks);
        root.addProperty("radius", -1);
        root.addProperty("sortOrder", sortOrder);
        root.addProperty("polarity", "negative");
        return root;
    }

    private static void appendEffects(JsonObject root, String[][] effects) {
        JsonArray effectsArray = new JsonArray();
        for (String[] entry : effects) {
            JsonObject eff = new JsonObject();
            eff.addProperty("effect", entry[0]);
            eff.addProperty("amplifier", Integer.parseInt(entry[1]));
            effectsArray.add(eff);
        }
        root.add("effects", effectsArray);
    }

    /**
     * Helper for writing a single-effect, mobile-only preset. Emits
     * {@code "tiers": ["mobile"]}. Ships icon/iconSelected refs so the mobile
     * picker screen added in 1.4.1 has something to render beyond the letter
     * fallback. 1.4.2: {@code showInSelector} is now {@code true} so the
     * stationary overlay can also show mobile presets when an instrument is
     * mapped to one (rare, but the fallback path in
     * {@link InstrumentAuraMapping#getAllowedAuras} otherwise hides them).
     */
    private static void writeMobileDefault(
            Path dir, String id, String displayName, String description,
            String color, int durationTicks, int sortOrder,
            String effectId, int amplifier
    ) throws IOException {
        Path file = dir.resolve(id + ".json");
        if (Files.exists(file)) return;
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
        root.addProperty("showInSelector", true);

        JsonArray effectsArray = new JsonArray();
        JsonObject eff = new JsonObject();
        eff.addProperty("effect", effectId);
        eff.addProperty("amplifier", amplifier);
        effectsArray.add(eff);
        root.add("effects", effectsArray);

        root.addProperty("icon", "effectiveinstruments:textures/gui/aura_" + id + ".png");
        root.addProperty("iconSelected", "effectiveinstruments:textures/gui/aura_" + id + "_selected.png");

        Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static void writeDefaultJson(Path dir, String id, String displayName, String description,
                                          String color, int durationTicks, int sortOrder,
                                          String[][] effects, String icon, String iconSelected) throws IOException {
        Path file = dir.resolve(id + ".json");
        // 1.4.4: preserve user edits. If the JSON already exists we skip it so
        // marker bumps don't clobber customizations. Fresh installs will still
        // write every preset because the stationary-defaults marker is absent.
        if (Files.exists(file)) return;
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

        Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
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

        // polarity (optional, 1.4.0+). Missing/unknown → positive (pre-1.4.0 behavior).
        Polarity polarity = parsePolarity(root, id);

        return new AuraPreset(
                id, displayName, description, color, effects,
                durationTicks, radius, enabled, sortOrder,
                iconTexture, selectedIconTexture,
                supportedTiers, showInSelector,
                polarity
        );
    }

    private static Polarity parsePolarity(JsonObject root, String auraId) {
        if (!root.has("polarity") || !root.get("polarity").isJsonPrimitive()) {
            return Polarity.POSITIVE;
        }
        String raw = root.get("polarity").getAsString();
        Polarity parsed = Polarity.fromJson(raw);
        if (parsed == null) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Aura '{}' declares unknown polarity '{}', defaulting to positive",
                    auraId, raw);
            return Polarity.POSITIVE;
        }
        return parsed;
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

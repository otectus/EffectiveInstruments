package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player mobile-tier aura selection, persisted as {@link SavedData} on the
 * overworld. Map layout: player UUID → instrument id → chosen aura id.
 *
 * <p>All access is on the main server thread (same concurrency contract as
 * {@link AuraManager}). The {@link #get(MinecraftServer)} accessor lazily
 * loads-or-creates the SavedData file, so callers don't have to thread the
 * server through every method.
 */
public final class MobilePlayerSelection extends SavedData {

    private static final String DATA_NAME = EffectiveInstrumentsMod.MODID + "_mobile_selections";

    // Outer: player UUID. Inner: instrument id → aura id.
    private final Map<UUID, Map<ResourceLocation, String>> selections = new HashMap<>();

    /**
     * Per-player throttle for {@code SelectAuraC2SPacket.handleMobile}. Mirrors
     * the 5-tick cooldown the stationary path uses via
     * {@code AuraManager.PlayerAuraState.lastSelectionTick}. Lives on the
     * SavedData so it shares lifecycle with the selection map proper, but is
     * NOT persisted (transient memory only — re-initialised on every server
     * boot, which is correct because game-time resets too).
     *
     * <p>Without this, a misbehaving client can flood mobile selections, each
     * of which calls {@link #setDirty()} and forces autosave I/O on every
     * packet.
     */
    private final transient Map<UUID, Long> lastMobileSelectionTick = new HashMap<>();

    public long getLastMobileSelectionTick(UUID playerId) {
        Long t = lastMobileSelectionTick.get(playerId);
        return t == null ? Long.MIN_VALUE : t;
    }

    public void markMobileSelectionTime(UUID playerId, long gameTick) {
        lastMobileSelectionTick.put(playerId, gameTick);
    }

    public void clearThrottle(UUID playerId) {
        lastMobileSelectionTick.remove(playerId);
    }

    /** Return the chosen aura id for a player+instrument pair, or {@code null} if none set. */
    @Nullable
    public String getSelection(UUID playerId, ResourceLocation instrumentId) {
        Map<ResourceLocation, String> byInstrument = selections.get(playerId);
        if (byInstrument == null) return null;
        return byInstrument.get(instrumentId);
    }

    /**
     * Store a selection. Passing {@code null} or an empty string clears the
     * selection for that player+instrument pair.
     */
    public void setSelection(UUID playerId, ResourceLocation instrumentId, @Nullable String auraId) {
        if (auraId == null || auraId.isEmpty()) {
            Map<ResourceLocation, String> byInstrument = selections.get(playerId);
            if (byInstrument != null) {
                byInstrument.remove(instrumentId);
                if (byInstrument.isEmpty()) selections.remove(playerId);
                setDirty();
            }
            return;
        }
        selections.computeIfAbsent(playerId, id -> new HashMap<>())
                .put(instrumentId, auraId);
        setDirty();
    }

    /** Clear every selection for a given player. Called on logout when the server config says so. */
    public void clearForPlayer(UUID playerId) {
        if (selections.remove(playerId) != null) {
            setDirty();
        }
    }

    // --- Persistence ---

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Map<ResourceLocation, String>> e : selections.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player", e.getKey());
            ListTag entries = new ListTag();
            for (Map.Entry<ResourceLocation, String> inst : e.getValue().entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putString("instrument", inst.getKey().toString());
                entry.putString("aura", inst.getValue());
                entries.add(entry);
            }
            playerTag.put("entries", entries);
            players.add(playerTag);
        }
        root.put("players", players);
        return root;
    }

    /** Factory used by {@link SavedData.Factory}. */
    public static MobilePlayerSelection load(CompoundTag tag) {
        MobilePlayerSelection out = new MobilePlayerSelection();
        if (!tag.contains("players", Tag.TAG_LIST)) return out;
        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag playerTag = players.getCompound(i);
            if (!playerTag.hasUUID("player")) continue;
            UUID id = playerTag.getUUID("player");
            Map<ResourceLocation, String> map = new HashMap<>();
            ListTag entries = playerTag.getList("entries", Tag.TAG_COMPOUND);
            for (int j = 0; j < entries.size(); j++) {
                CompoundTag entry = entries.getCompound(j);
                String instrument = entry.getString("instrument");
                String aura = entry.getString("aura");
                if (instrument.isEmpty() || aura.isEmpty()) continue;
                if (!ResourceLocation.isValidResourceLocation(instrument)) continue;
                map.put(new ResourceLocation(instrument), aura);
            }
            if (!map.isEmpty()) out.selections.put(id, map);
        }
        return out;
    }

    /**
     * Resolve-or-create the singleton {@link MobilePlayerSelection} on the
     * overworld's data-storage. Never returns null on a live server.
     */
    public static MobilePlayerSelection get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            // Unusual — server hasn't finished booting. Give back an orphan instance
            // so callers don't NPE; writes won't persist, but reads return "no
            // selection" which is the correct fallback behavior.
            return new MobilePlayerSelection();
        }
        return overworld.getDataStorage().computeIfAbsent(
                MobilePlayerSelection::load,
                MobilePlayerSelection::new,
                DATA_NAME
        );
    }
}

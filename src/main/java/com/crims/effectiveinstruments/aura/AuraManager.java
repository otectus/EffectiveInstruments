package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.network.EIPacketHandler;
import com.crims.effectiveinstruments.network.packet.SyncAuraSelectionS2CPacket;
import com.crims.effectiveinstruments.particle.AuraNoteParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.*;

public class AuraManager {
    // All access occurs on the main server thread (tick handler, enqueueWork packet handlers, Forge events).
    private static final Map<UUID, PlayerAuraState> PLAYER_STATES = new HashMap<>();
    private static final Set<UUID> activeMusicians = new HashSet<>();

    public static class PlayerAuraState {
        @Nullable
        AuraPreset selectedAura;
        @Nullable
        ResourceLocation currentInstrumentId;
        long lastNoteGameTime = -1;
        long lastSelectionTick = -1;
        long lastOpenPacketTick = -1;
        // Sliding window of note timestamps for the activation-threshold check.
        // Bounded in practice by NOTE_THRESHOLD_WINDOW_TICKS * note rate.
        final Deque<Long> recentNoteTicks = new ArrayDeque<>();
        // Authoritative instrument-open flag — set only by the server-side
        // InstrumentOpenStateChangedEvent handler, never by a client packet.
        boolean instrumentOpen = false;
        // entity network ID -> set of effects we applied to that entity
        final Map<Integer, Set<MobEffect>> affectedTargets = new HashMap<>();

        @Nullable
        public AuraPreset getSelectedAura() { return selectedAura; }

        @Nullable
        public ResourceLocation getCurrentInstrumentId() { return currentInstrumentId; }

        public boolean isInstrumentOpen() { return instrumentOpen; }

        public int getAffectedTargetCount() { return affectedTargets.size(); }

        public long getLastSelectionTick() { return lastSelectionTick; }

        public void markSelectionTime(long gameTick) { this.lastSelectionTick = gameTick; }

        public long getLastOpenPacketTick() { return lastOpenPacketTick; }

        public void markOpenPacketTime(long gameTick) { this.lastOpenPacketTick = gameTick; }

        /** Record a note event and prune stale entries beyond the threshold window. */
        void recordNote(long gameTick) {
            recentNoteTicks.addLast(gameTick);
            long windowStart = gameTick - EIServerConfig.NOTE_THRESHOLD_WINDOW_TICKS.get();
            while (!recentNoteTicks.isEmpty() && recentNoteTicks.peekFirst() < windowStart) {
                recentNoteTicks.removeFirst();
            }
            this.lastNoteGameTime = gameTick;
        }

        boolean isActive(long currentGameTime) {
            if (selectedAura == null || !instrumentOpen) return false;
            long window = EIServerConfig.NOTE_WINDOW_TICKS.get();
            if ((currentGameTime - lastNoteGameTime) > window) return false;
            // Activation threshold: require at least N notes in the sliding window.
            // Prune stale entries so the check reflects *current* play, not ancient history.
            long thresholdWindowStart = currentGameTime - EIServerConfig.NOTE_THRESHOLD_WINDOW_TICKS.get();
            while (!recentNoteTicks.isEmpty() && recentNoteTicks.peekFirst() < thresholdWindowStart) {
                recentNoteTicks.removeFirst();
            }
            return recentNoteTicks.size() >= EIServerConfig.NOTE_THRESHOLD_MIN.get();
        }
    }

    // --- State mutation ---

    public static void setAuraSelection(ServerPlayer player, AuraPreset preset) {
        getOrCreate(player.getUUID()).selectedAura = preset;
        EffectiveInstrumentsMod.LOGGER.debug("Player {} selected aura: {}", player.getName().getString(), preset.id());
    }

    public static void clearAuraSelection(ServerPlayer player) {
        getOrCreate(player.getUUID()).selectedAura = null;
        EffectiveInstrumentsMod.LOGGER.debug("Player {} deselected aura", player.getName().getString());
    }

    public static void onNotePlayed(ServerPlayer player) {
        PlayerAuraState state = getOrCreate(player.getUUID());
        state.recordNote(player.level().getGameTime());
    }

    /**
     * Authoritative "instrument opened" signal. Called only from the server-side
     * {@link com.cstav.genshinstrument.event.InstrumentOpenStateChangedEvent} handler.
     * Client packets can annotate state but must never set this flag directly.
     */
    public static void onInstrumentOpen(Player player) {
        getOrCreate(player.getUUID()).instrumentOpen = true;
        activeMusicians.add(player.getUUID());
    }

    /**
     * Non-authoritative annotation from the client {@code InstrumentOpenC2SPacket}.
     * Records which instrument the player has open and, if a default aura is
     * configured for that instrument, auto-selects it. Does NOT toggle the
     * authoritative open flag or register the player as an active musician —
     * those transitions are reserved for {@link #onInstrumentOpen}.
     */
    public static void onInstrumentIdReceived(ServerPlayer player, ResourceLocation instrumentId) {
        PlayerAuraState state = getOrCreate(player.getUUID());
        state.currentInstrumentId = instrumentId;

        // Auto-select default aura for this instrument
        String defaultAuraId = InstrumentAuraMapping.getDefaultAuraId(instrumentId);
        if (defaultAuraId != null) {
            Optional<AuraPreset> preset = AuraRegistry.getById(defaultAuraId);
            if (preset.isPresent() && preset.get().enabled()) {
                onAuraSwitch(player);
                setAuraSelection(player, preset.get());
                EIPacketHandler.sendToPlayer(new SyncAuraSelectionS2CPacket(defaultAuraId), player);
                return;
            }
        }
        // No default — notify client
        EIPacketHandler.sendToPlayer(new SyncAuraSelectionS2CPacket(""), player);
    }

    public static void onInstrumentClose(Player player) {
        PlayerAuraState state = getOrCreate(player.getUUID());
        if (state.instrumentOpen && player instanceof ServerPlayer sp) {
            onAuraSwitch(sp);
            clearAuraSelection(sp);
        }
        state.instrumentOpen = false;
        state.currentInstrumentId = null;
        activeMusicians.remove(player.getUUID());
    }

    public static void onPlayerLogout(UUID playerId) {
        activeMusicians.remove(playerId);
        PLAYER_STATES.remove(playerId);
    }

    @Nullable
    public static PlayerAuraState getState(UUID playerId) {
        return PLAYER_STATES.get(playerId);
    }

    // --- Tick handler ---

    public static void onServerTick(ServerLevel level) {
        if (!EIServerConfig.ENABLED.get()) return;

        long gameTime = level.getGameTime();
        int interval = EIServerConfig.AURA_TICK_INTERVAL.get();

        if (gameTime % interval != 0) return;

        boolean debug = EIServerConfig.DEBUG_MODE.get();
        long tickStartNanos = debug ? System.nanoTime() : 0L;
        int activeCount = 0;
        int totalTargetCount = 0;

        // Iterate only active musicians instead of all players in the level
        for (UUID musicianId : List.copyOf(activeMusicians)) {
            PlayerAuraState state = PLAYER_STATES.get(musicianId);
            if (state == null || !state.isActive(gameTime)) continue;

            // Resolve player from this specific level; null if player is in a different dimension
            ServerPlayer player = (ServerPlayer) level.getPlayerByUUID(musicianId);
            if (player == null) continue;

            AuraPreset aura = state.selectedAura;
            if (aura == null) continue;

            // Validate the aura still exists and is enabled after a potential reload
            Optional<AuraPreset> current = AuraRegistry.getById(aura.id());
            if (current.isEmpty() || !current.get().enabled()) {
                clearPreviousAuraEffects(player, state);
                state.selectedAura = null;
                EffectiveInstrumentsMod.LOGGER.debug(
                        "Cleared stale aura selection '{}' for player {}",
                        aura.id(), player.getName().getString());
                continue;
            }

            applyAuraEffects(player, aura);
            spawnAuraParticles(player, aura);

            if (debug) {
                activeCount++;
                totalTargetCount += state.getAffectedTargetCount();
            }
        }

        if (debug && activeCount > 0) {
            long elapsedMicros = (System.nanoTime() - tickStartNanos) / 1000L;
            EffectiveInstrumentsMod.LOGGER.info(
                    "[EI debug] tick={} musicians={} totalTargets={} elapsedUs={}",
                    gameTime, activeCount, totalTargetCount, elapsedMicros);
        }
    }

    private static void applyAuraEffects(ServerPlayer source, AuraPreset aura) {
        int radius = aura.getEffectiveRadius();
        int duration = aura.getEffectiveDuration();
        PlayerAuraState state = getOrCreate(source.getUUID());
        AuraApplicator.apply(source, aura, radius, duration, stationaryProfile(), state.affectedTargets);
    }

    private static TargetingProfile stationaryProfile() {
        return new TargetingProfile(
                EIServerConfig.ALLOW_SELF_BUFF.get(),
                EIServerConfig.INCLUDE_OTHER_PLAYERS.get(),
                EIServerConfig.INCLUDE_TAMED_PETS.get(),
                EIServerConfig.MAX_TARGETS_PER_TICK.get()
        );
    }

    /**
     * Called when the musician switches to a different aura.
     * Strips old aura effects from all tracked targets.
     */
    public static void onAuraSwitch(ServerPlayer player) {
        PlayerAuraState state = PLAYER_STATES.get(player.getUUID());
        if (state == null) return;
        clearPreviousAuraEffects(player, state);
    }

    // Note: When called after a dimension change, musician.serverLevel() returns the NEW dimension.
    // Entity lookups for targets in the OLD dimension will return null, so those effects are not
    // actively cleaned up. This is acceptable — effects expire naturally within 13 seconds (260 ticks max).
    private static void clearPreviousAuraEffects(ServerPlayer musician, PlayerAuraState state) {
        AuraPreset oldAura = state.selectedAura;
        if (oldAura == null) return;
        // Effects with much longer remaining duration than our aura's are likely from other sources
        int maxExpectedDuration = oldAura.getEffectiveDuration() + EIServerConfig.AURA_TICK_INTERVAL.get() * 2;
        AuraApplicator.clear(musician.serverLevel(), oldAura, maxExpectedDuration, state.affectedTargets);
    }

    private static void spawnAuraParticles(ServerPlayer source, AuraPreset aura) {
        ServerLevel level = source.serverLevel();
        int radius = aura.getEffectiveRadius();
        int color = aura.color();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        AuraNoteParticleOptions options = new AuraNoteParticleOptions(r, g, b);
        int count = Math.min(3 + radius / 4, 12);

        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = radius <= 1
                    ? level.random.nextDouble() * Math.max(radius, 0.5)
                    : 1.0 + level.random.nextDouble() * (radius - 1);
            double px = source.getX() + Math.cos(angle) * dist;
            double py = source.getY() + 0.5 + level.random.nextDouble() * 2.0;
            double pz = source.getZ() + Math.sin(angle) * dist;

            level.sendParticles(options, px, py, pz, 1, 0, 0.02, 0, 0);
        }
    }

    /**
     * Thin wrapper used by the mobile tier (and future callers) to check whether
     * a player currently has an active stationary aura without reaching into
     * {@link PlayerAuraState}. Returns false when the player has no tracked state.
     */
    public static boolean isActive(UUID playerId, long currentGameTime) {
        PlayerAuraState state = PLAYER_STATES.get(playerId);
        return state != null && state.isActive(currentGameTime);
    }

    private static PlayerAuraState getOrCreate(UUID playerId) {
        return PLAYER_STATES.computeIfAbsent(playerId, k -> new PlayerAuraState());
    }
}

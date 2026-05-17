package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.network.EIPacketHandler;
import com.crims.effectiveinstruments.network.packet.SyncAuraSelectionS2CPacket;
import com.crims.effectiveinstruments.particle.AuraNoteParticleOptions;
import com.crims.effectiveinstruments.performer.IAuraPerformer;
import com.crims.effectiveinstruments.performer.PerformerTier;
import com.crims.effectiveinstruments.performer.PlayerPerformer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
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
        /**
         * 1.4.9 (RECS §2.7): last server tick on which {@link #affectedTargets}
         * was pruned of dead-entity ids. Pruning is amortized to once per
         * minute (1200 ticks) — without it the map grows unbounded with
         * dead-mob ids in long sessions where the same aura keeps firing.
         */
        long lastPruneTick = Long.MIN_VALUE;
        // Sliding window of note timestamps for the activation-threshold check.
        // Bounded in practice by NOTE_THRESHOLD_WINDOW_TICKS * note rate.
        final Deque<Long> recentNoteTicks = new ArrayDeque<>();
        /**
         * Mirrors the client's open-screen state for diagnostics; not load-
         * bearing for aura activation. Set by the backend-specific instrument-
         * open event handler (e.g. {@code GenshinInstrumentEventHandler} when
         * GI is installed), cleared on close. Reads in
         * /effectiveinstruments status + diagnose only — kept so future
         * reintroduction of a screen-state-aware feature has a hook ready.
         */
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
            if (selectedAura == null) return false;
            // 1.4.7: removed the `!instrumentOpen` gate. Recent notes are the
            // authoritative signal of "player is actively musicianing"; the
            // screen-open flag is redundant and caused the aura to immediately
            // deactivate when users closed the screen to observe effects. The
            // note-window (default 5s) provides a natural linger.
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
        onNotePlayed((IAuraPerformer) new PlayerPerformer(player, PerformerTier.STATIONARY));
    }

    /**
     * 1.6.0 hotfix #4: performer-aware overload. Used by NPC adapter goals
     * (Recruits, Touhou Maid, Iron's Spells summons, …) to record a played
     * note. State is keyed by {@code performer.entity().getUUID()} — players
     * and NPCs share the same map.
     *
     * <p>For NPCs without an explicit aura selection (no UI to set one), this
     * auto-selects the held instrument's default aura via
     * {@link InstrumentAuraMapping#getDefaultAuraId(ResourceLocation)} — the
     * same mapping the player path uses on instrument-screen-open. Without
     * this, NPCs would never enter PLAYING state because
     * {@code state.selectedAura} would remain null.
     */
    public static void onNotePlayed(IAuraPerformer performer) {
        LivingEntity entity = performer.entity();
        PlayerAuraState state = getOrCreate(entity.getUUID());
        state.recordNote(entity.level().getGameTime());
        activeMusicians.add(entity.getUUID());

        // NPC auto-select: only when no aura is set. Players use the UI; NPCs
        // have none, so we mirror onInstrumentIdReceived's lookup here.
        if (state.selectedAura == null && !performer.isPlayer()) {
            autoSelectFromInstrument(state, performer);
        }
    }

    /**
     * Look up the default aura for the performer's held instrument and pin it
     * to {@code state.selectedAura}. Used by the NPC paths; no-op when no
     * mapping exists for the held instrument.
     */
    private static void autoSelectFromInstrument(PlayerAuraState state, IAuraPerformer performer) {
        net.minecraft.world.item.ItemStack stack = performer.instrumentStack();
        if (stack.isEmpty()) return;
        ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return;
        String defaultId = InstrumentAuraMapping.getDefaultAuraId(itemId);
        if (defaultId == null) return;
        AuraRegistry.getById(defaultId).ifPresent(preset -> {
            state.selectedAura = preset;
            state.currentInstrumentId = itemId;
        });
    }

    /**
     * Apply the player's currently-selected aura to nearby targets right now.
     * Called on every note played (stationary tier) so the visual + mechanical
     * effect lands within milliseconds of the first note, not on the next tick.
     * No-ops cleanly if the player has no state, no selection, no active notes,
     * or the aura's master gate is off. Does NOT spawn particles — those stay
     * on the tick-scheduled path to avoid per-note particle spam.
     */
    public static void applyAuraNow(ServerPlayer player) {
        applyAuraNow((IAuraPerformer) new PlayerPerformer(player, PerformerTier.STATIONARY));
    }

    /**
     * 1.6.0: performer-aware overload. Used by NPC adapter goals (Recruits,
     * Touhou Maid, etc.) to apply the selected aura on every note. State is
     * keyed by {@code performer.entity().getUUID()} so a single map handles
     * both player and NPC entries — no separate NPC map.
     */
    public static void applyAuraNow(IAuraPerformer performer) {
        LivingEntity entity = performer.entity();
        // Player path stays byte-identical to 1.5.0: use get(), not getOrCreate,
        // so state is null until the player explicitly opens an instrument or
        // selects an aura. NPCs use getOrCreate so the state exists by the time
        // applyAuraNow runs even if onNotePlayed wasn't called first (defense
        // in depth against goal-order races).
        PlayerAuraState state = performer.isPlayer()
                ? PLAYER_STATES.get(entity.getUUID())
                : getOrCreate(entity.getUUID());
        if (state == null) return;
        if (state.selectedAura == null && !performer.isPlayer()) {
            autoSelectFromInstrument(state, performer);
        }
        AuraPreset aura = state.selectedAura;
        if (aura == null) return;
        long gameTime = entity.level().getGameTime();
        if (!state.isActive(gameTime)) return;
        Optional<AuraPreset> current = AuraRegistry.getById(aura.id());
        if (current.isEmpty() || !current.get().enabled()) return;
        applyAuraEffects(performer, aura);
    }

    /**
     * Authoritative "instrument opened" signal. Called only from the server-side
     * backend-specific instrument-open event handler — for Genshin Instruments
     * that is {@code compat.genshin.GenshinInstrumentEventHandler}, registered
     * conditionally when GI is installed. Client packets can annotate state but
     * must never set this flag directly.
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
        // 1.4.9 (RECS §3.2): drop the `instrumentOpen && ...` gate. A player
        // who triggered the aura via onNotePlayed (which doesn't flip
        // instrumentOpen) and then closed the instrument used to leave their
        // tracked targets buffed until the effects expired naturally — the
        // gate suppressed the cleanup pass. Now we always clear on close.
        if (player instanceof ServerPlayer sp) {
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

            // 1.4.9 (RECS §2.7): amortized prune of dead-entity ids. Once
            // per minute keeps the map bounded in long sessions without
            // adding any per-tick scan cost.
            if (gameTime - state.lastPruneTick >= 1200) {
                state.lastPruneTick = gameTime;
                state.affectedTargets.keySet().removeIf(id -> level.getEntity(id) == null);
            }

            applyAuraEffects(player, aura);
            spawnAuraParticles(player, aura);

            if (debug) {
                activeCount++;
                totalTargetCount += state.getAffectedTargetCount();
                EffectiveInstrumentsMod.LOGGER.info(
                        "[EI debug] aura={} offensive={} targets={} player={}",
                        aura.id(), aura.isOffensive(),
                        state.getAffectedTargetCount(),
                        player.getName().getString());
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
        applyAuraEffects((IAuraPerformer) new PlayerPerformer(source, PerformerTier.STATIONARY), aura);
    }

    /**
     * 1.6.0: performer-aware overload. Builds the same TargetingProfile +
     * radius/duration + calls AuraApplicator with the IAuraPerformer overload.
     */
    private static void applyAuraEffects(IAuraPerformer source, AuraPreset aura) {
        if (aura.isOffensive() && !EIServerConfig.OFFENSIVE_AURAS_ENABLED.get()) {
            // Master kill switch — treat as if the preset doesn't exist at the apply stage.
            return;
        }
        int radius = aura.getEffectiveRadius();
        int duration = aura.getEffectiveDuration();
        PlayerAuraState state = getOrCreate(source.entity().getUUID());
        TargetingProfile profile = aura.isOffensive()
                ? TargetingProfiles.offensive()
                : TargetingProfiles.positive();
        AuraApplicator.apply(source, aura, radius, duration, profile, state.affectedTargets);
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
        spawnAuraNotes(source, aura, aura.getEffectiveRadius());
    }

    /**
     * Spawn the aura-note particle plume around {@code source}. Shared entry
     * point for the stationary tier ({@link #spawnAuraParticles}) and the
     * mobile tier (Immersive Melodies). Caller supplies the radius because
     * the mobile tier uses a shorter, config-driven radius distinct from
     * {@link AuraPreset#getEffectiveRadius()}.
     */
    public static void spawnAuraNotes(ServerPlayer source, AuraPreset aura, int radius) {
        spawnAuraNotes((LivingEntity) source, aura, radius);
    }

    /**
     * 1.6.0: entity-generic overload. NPC adapters call this so their
     * performers emit the same aura-note plume as players. Particle origin
     * shifts up to {@code y + bbHeight * 0.7} (about head height for vanilla-
     * sized mobs) per spec §11, but for players this keeps the {@code y + 0.5}
     * offset for byte-identical visual parity.
     */
    public static void spawnAuraNotes(LivingEntity source, AuraPreset aura, int radius) {
        if (!(source.level() instanceof ServerLevel level)) return;
        int color = aura.color();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        AuraNoteParticleOptions options = new AuraNoteParticleOptions(r, g, b);
        int count = Math.min(3 + radius / 4, 12);

        // Match the legacy player offset of +0.5 above feet for ServerPlayer;
        // for NPCs use head-height (bbHeight * 0.7) so the plume reads from
        // their position regardless of mob size.
        double yBase = source instanceof ServerPlayer ? 0.5 : source.getBbHeight() * 0.7;

        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = radius <= 1
                    ? level.random.nextDouble() * Math.max(radius, 0.5)
                    : 1.0 + level.random.nextDouble() * (radius - 1);
            double px = source.getX() + Math.cos(angle) * dist;
            double py = source.getY() + yBase + level.random.nextDouble() * 2.0;
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

    /** Alias used by {@code /effectiveinstruments diagnose} — external readers shouldn't take a hard dep on {@link #isActive}'s package-private meaning. */
    public static boolean isActiveTest(UUID playerId, long currentGameTime) {
        return isActive(playerId, currentGameTime);
    }

    private static PlayerAuraState getOrCreate(UUID playerId) {
        return PLAYER_STATES.computeIfAbsent(playerId, k -> new PlayerAuraState());
    }
}

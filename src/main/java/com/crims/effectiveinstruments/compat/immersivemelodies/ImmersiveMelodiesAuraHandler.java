package com.crims.effectiveinstruments.compat.immersivemelodies;

import com.crims.effectiveinstruments.aura.AuraApplicator;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.aura.TargetingProfile;
import com.crims.effectiveinstruments.config.EIServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mobile tier tick loop. Runs after {@link AuraManager#onServerTick} so the
 * suppression check can read fresh stationary state.
 *
 * <p>All access is on the main server thread, same as {@link AuraManager}.
 *
 * <p>Known limitation (1.3.0): only reacts to IM autoplay / selected-melody
 * playback (the state that flips {@code playing=true} on the item stack).
 * Free-play keyboard/MIDI mode does not qualify — see
 * {@link ImmersiveMelodiesCompat} class docs.
 */
public final class ImmersiveMelodiesAuraHandler {

    private static final Map<UUID, MobileAuraState> STATES = new HashMap<>();

    /** Per-player mobile aura state. Lives only here — no public surface. */
    static final class MobileAuraState {
        @Nullable
        AuraPreset activeAura;
        @Nullable
        ResourceLocation instrumentId;
        long lastActiveTick = Long.MIN_VALUE;
        final Map<Integer, Set<MobEffect>> affectedTargets = new HashMap<>();
    }

    // --- Tick entry point (wired from InstrumentStateHandler) ---

    public static void onServerTick(ServerLevel level) {
        if (!ImmersiveMelodiesCompat.isAvailable()) return;
        if (!EIServerConfig.MOBILE_TIER_ENABLED.get()) return;

        long gameTime = level.getGameTime();
        int pulse = EIServerConfig.MOBILE_PULSE_INTERVAL_TICKS.get();
        if (gameTime % pulse != 0) return;

        for (ServerPlayer player : level.players()) {
            tickPlayer(level, player, gameTime);
        }
    }

    private static void tickPlayer(ServerLevel level, ServerPlayer player, long gameTime) {
        MobileAuraState state = STATES.computeIfAbsent(player.getUUID(), id -> new MobileAuraState());

        // Suppression: stationary tier wins.
        if (EIServerConfig.SUPPRESS_MOBILE_WHEN_STATIONARY_ACTIVE.get()
                && AuraManager.isActive(player.getUUID(), gameTime)) {
            clearImmediate(level, state);
            return;
        }

        ImmersiveMelodiesCompat.HeldInstrument held =
                ImmersiveMelodiesCompat.findActivePlayingStack(player);

        if (held == null) {
            handleIdle(level, state, gameTime);
            return;
        }

        AuraPreset aura = MobileInstrumentAuraMapping.resolve(held.instrumentId());
        if (aura == null) {
            handleIdle(level, state, gameTime);
            return;
        }

        // Switch cleanup: if the resolved aura changed, strip the old one before applying the new.
        if (state.activeAura != null && !state.activeAura.id().equals(aura.id())) {
            AuraApplicator.clear(
                    level,
                    state.activeAura,
                    maxExpectedDurationFor(state.activeAura),
                    state.affectedTargets
            );
        }

        state.activeAura = aura;
        state.instrumentId = held.instrumentId();
        state.lastActiveTick = gameTime;

        int radius = resolveMobileRadius(aura);
        int duration = aura.getEffectiveDuration();
        AuraApplicator.apply(
                player,
                aura,
                radius,
                duration,
                mobileProfile(),
                state.affectedTargets
        );
    }

    /**
     * Player has no playing IM stack this pulse. Start (or continue) the linger
     * countdown; once it expires, strip effects from tracked targets.
     */
    private static void handleIdle(ServerLevel level, MobileAuraState state, long gameTime) {
        if (state.activeAura == null) return;

        int linger = EIServerConfig.MOBILE_LINGER_TICKS.get();
        if (gameTime - state.lastActiveTick < linger) return;

        AuraApplicator.clear(
                level,
                state.activeAura,
                maxExpectedDurationFor(state.activeAura),
                state.affectedTargets
        );
        state.activeAura = null;
        state.instrumentId = null;
    }

    /** Immediate clear (suppression / explicit lifecycle clear), no linger. */
    private static void clearImmediate(ServerLevel level, MobileAuraState state) {
        if (state.activeAura == null && state.affectedTargets.isEmpty()) return;
        AuraApplicator.clear(
                level,
                state.activeAura,
                maxExpectedDurationFor(state.activeAura),
                state.affectedTargets
        );
        // Defensive: if activeAura was null but affectedTargets was non-empty,
        // AuraApplicator.clear early-returns without clearing the map. Ensure
        // no orphaned entries survive.
        state.affectedTargets.clear();
        state.activeAura = null;
        state.instrumentId = null;
    }

    // --- Lifecycle hooks (wired from InstrumentStateHandler) ---

    /** Drop all per-player state. Called on logout. No effect-strip needed — the player is gone. */
    public static void onPlayerLogout(UUID playerId) {
        STATES.remove(playerId);
    }

    /**
     * Force-clear mobile effects and state for a player on the current tick.
     * Called on death so respawned players don't retain ghost mobile buffs.
     */
    public static void onExplicitClear(ServerPlayer player) {
        MobileAuraState state = STATES.get(player.getUUID());
        if (state == null) return;
        clearImmediate(player.serverLevel(), state);
    }

    // --- Helpers ---

    private static TargetingProfile mobileProfile() {
        return new TargetingProfile(
                EIServerConfig.MOBILE_ALLOW_SELF_BUFF.get(),
                EIServerConfig.MOBILE_INCLUDE_OTHER_PLAYERS.get(),
                EIServerConfig.MOBILE_INCLUDE_TAMED_PETS.get(),
                EIServerConfig.MOBILE_MAX_TARGETS_PER_TICK.get()
        );
    }

    /**
     * Resolve radius for a mobile preset. Mirrors the intent of
     * {@link AuraPreset#getEffectiveRadius()} but uses the mobile default instead
     * of the stationary one when the JSON radius is -1.
     */
    private static int resolveMobileRadius(AuraPreset aura) {
        int override = aura.radiusOverride();
        if (override >= 0) return override;
        return EIServerConfig.MOBILE_DEFAULT_RADIUS.get();
    }

    private static int maxExpectedDurationFor(@Nullable AuraPreset aura) {
        if (aura == null) return 0;
        return aura.getEffectiveDuration() + EIServerConfig.MOBILE_PULSE_INTERVAL_TICKS.get() * 2;
    }

    // --- Testing / status query ---

    /** Read-only view of a player's current mobile state (for status command + API). */
    @Nullable
    public static MobileStateView getView(UUID playerId) {
        MobileAuraState state = STATES.get(playerId);
        if (state == null || state.activeAura == null) return null;
        return new MobileStateView(state.instrumentId, state.activeAura.id(), state.affectedTargets.size());
    }

    /** Immutable snapshot returned by {@link #getView}. */
    public record MobileStateView(
            @Nullable ResourceLocation instrumentId,
            String auraId,
            int affectedTargetCount
    ) {}

    private ImmersiveMelodiesAuraHandler() {}
}

package com.crims.effectiveinstruments.compat.immersivemelodies;

import com.crims.effectiveinstruments.aura.AuraApplicator;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.aura.TargetingProfile;
import com.crims.effectiveinstruments.aura.TargetingProfiles;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mobile tier tick loop. Runs after {@link AuraManager#onServerTick} so the
 * suppression check can read fresh stationary state.
 *
 * <p>All access is on the main server thread, same as {@link AuraManager}.
 *
 * <p>Activation paths (1.4.3+):
 * <ul>
 *   <li><b>NBT-driven</b> — IM autoplay or selected-melody playback flips
 *       {@code playing=true} on the held stack;
 *       {@link ImmersiveMelodiesCompat#findActivePlayingStack} surfaces it.</li>
 *   <li><b>Screen-open</b> — any IM screen open (including free-play
 *       keyboard / MIDI) records {@code screenOpenInstrumentId} on the
 *       per-player state via {@link #onScreenOpened}, and the tick loop
 *       applies the aura even without {@code playing=true}.</li>
 * </ul>
 *
 * <p>Free-play used to be a "known limitation" in 1.3.0. The screen-open
 * path closed that gap; durability is still only charged on the held-and-
 * playing path because browsing a melody list shouldn't wear the instrument.
 */
public final class ImmersiveMelodiesAuraHandler {

    private static final Map<UUID, MobileAuraState> STATES = new HashMap<>();

    /**
     * Per-player throttle for mobile {@code InstrumentOpenC2SPacket} traffic.
     * Mirrors the stationary 5-tick cooldown on
     * {@code AuraManager.PlayerAuraState.lastOpenPacketTick}. Cleared on
     * {@link #onPlayerLogout} so dropped players don't pin entries forever.
     * Without this, a flood of open/close packets would thrash the
     * screen-open activation gate (and on a public multiplayer server,
     * the corresponding active-musician membership churn).
     */
    private static final Map<UUID, Long> LAST_OPEN_PACKET_TICK = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Returns true and records the tick when the packet should be honored.
     * Returns false (no state change) when the packet falls inside the 5-tick
     * cooldown window from the previous accepted packet for this player.
     */
    public static boolean acceptOpenPacket(UUID playerId, long gameTime) {
        Long last = LAST_OPEN_PACKET_TICK.get(playerId);
        if (last != null && gameTime - last < 5) return false;
        LAST_OPEN_PACKET_TICK.put(playerId, gameTime);
        return true;
    }

    /** Per-player mobile aura state. Lives only here — no public surface. */
    static final class MobileAuraState {
        @Nullable
        AuraPreset activeAura;
        @Nullable
        ResourceLocation instrumentId;
        long lastActiveTick = Long.MIN_VALUE;
        /**
         * 1.4.9 (RECS §2.7): last server tick on which {@link #affectedTargets}
         * was pruned of dead-entity ids. The prune is amortized to once per
         * minute (1200 ticks) — without it the map grows unbounded with dead
         * mob ids in long sessions where the player keeps the same aura.
         */
        long lastPruneTick = Long.MIN_VALUE;
        final Map<Integer, Set<MobEffect>> affectedTargets = new HashMap<>();
        /**
         * Instrument ID whose IM screen is currently open on this player's client.
         * Set by {@link #onScreenOpened} / cleared by {@link #onScreenClosed} —
         * lets the tick loop apply the mobile aura even when IM's
         * {@code playing=true} NBT flag isn't set (free-play mode). Null means
         * no IM screen is open for this player.
         */
        @Nullable
        ResourceLocation screenOpenInstrumentId;
    }

    /**
     * 1.4.9 (RECS §2.6): players currently considered "actively using a mobile
     * instrument". Source of truth for the per-tick scan loop — without this,
     * {@link #onServerTick} would walk every player on the server every pulse
     * even when nobody's playing an IM instrument. Membership is granted by
     * {@link #onScreenOpened} (free-play path) and by the per-pulse discovery
     * scan when an NBT-marked playing stack is found in either hand. Removed
     * from the set on {@link #onScreenClosed} (when no held playing stack is
     * found), {@link #onPlayerLogout}, and after {@code MOBILE_LINGER_TICKS}
     * of idle.
     */
    private static final Set<UUID> ACTIVE_MOBILE_MUSICIANS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 1.4.9 (RECS §2.6): how often to do a full discovery scan for players
     * with NBT-marked playing stacks who aren't in {@link #ACTIVE_MOBILE_MUSICIANS}
     * yet (e.g. they triggered IM autoplay without ever opening an IM screen
     * this session). Cheap — runs at the same cadence as the pulse interval
     * but only every Nth pulse.
     */
    private static final int DISCOVERY_PULSE_INTERVAL = 4;
    private static long lastDiscoveryGameTime = Long.MIN_VALUE;

    // --- Tick entry point (wired from InstrumentStateHandler) ---

    public static void onServerTick(ServerLevel level) {
        if (!ImmersiveMelodiesCompat.isAvailable()) return;
        if (!EIServerConfig.MOBILE_TIER_ENABLED.get()) return;

        long gameTime = level.getGameTime();
        int pulse = EIServerConfig.MOBILE_PULSE_INTERVAL_TICKS.get();
        if (gameTime % pulse != 0) return;

        // 1.4.9 (RECS §2.6): scan only active mobile musicians, not every
        // player on the level. Membership is granted by onScreenOpened (free-
        // play path) and the periodic discovery scan below for the NBT-marked
        // autoplay path. On a server with zero IM users this loop is empty.
        if (!ACTIVE_MOBILE_MUSICIANS.isEmpty()) {
            // Snapshot to a list — tickPlayer can call clearImmediate which
            // removes from the set, ConcurrentModificationException-safe but
            // we want stable iteration anyway.
            for (UUID uuid : List.copyOf(ACTIVE_MOBILE_MUSICIANS)) {
                ServerPlayer p = level.getServer() == null
                        ? null
                        : level.getServer().getPlayerList().getPlayer(uuid);
                if (p == null || p.serverLevel() != level) continue;
                tickPlayer(level, p, gameTime);
            }
        }

        // Periodic discovery — pick up players who triggered IM autoplay
        // without ever opening the IM screen this session. Once they're
        // detected they get added to ACTIVE_MOBILE_MUSICIANS and the cheap
        // path above takes over.
        long pulsesPerDiscovery = (long) DISCOVERY_PULSE_INTERVAL * pulse;
        if (gameTime - lastDiscoveryGameTime >= pulsesPerDiscovery) {
            lastDiscoveryGameTime = gameTime;
            for (ServerPlayer p : level.players()) {
                if (ACTIVE_MOBILE_MUSICIANS.contains(p.getUUID())) continue;
                if (ImmersiveMelodiesCompat.findActivePlayingStack(p) != null) {
                    ACTIVE_MOBILE_MUSICIANS.add(p.getUUID());
                    tickPlayer(level, p, gameTime);
                }
            }
        }
    }

    private static void tickPlayer(ServerLevel level, ServerPlayer player, long gameTime) {
        MobileAuraState state = STATES.computeIfAbsent(player.getUUID(), id -> new MobileAuraState());

        // 1.4.9 (RECS §2.7): amortized prune of dead-entity ids from
        // affectedTargets. Once a minute is plenty — the map only grows when
        // mobs die mid-aura, and the cost of a missed prune is just a few
        // extra hash buckets, not a correctness issue.
        if (gameTime - state.lastPruneTick >= 1200) {
            state.lastPruneTick = gameTime;
            state.affectedTargets.keySet().removeIf(id -> level.getEntity(id) == null);
        }

        // Suppression: stationary tier wins.
        if (EIServerConfig.SUPPRESS_MOBILE_WHEN_STATIONARY_ACTIVE.get()
                && AuraManager.isActive(player.getUUID(), gameTime)) {
            clearImmediate(level, state);
            ACTIVE_MOBILE_MUSICIANS.remove(player.getUUID());
            return;
        }

        // Two valid activation paths for a mobile aura this pulse:
        //   (1) player holds an IM instrument with playing=true NBT (IM autoplay).
        //   (2) player has an IM screen open — tracked via onScreenOpened from
        //       AuraOverlayInjector. Covers free-play mode where IM never sets
        //       playing=true, which is what the user reported as "clicking the
        //       picker does nothing".
        ImmersiveMelodiesCompat.HeldInstrument held =
                ImmersiveMelodiesCompat.findActivePlayingStack(player);
        ResourceLocation activeInstrumentId = held != null ? held.instrumentId() : state.screenOpenInstrumentId;
        net.minecraft.world.item.ItemStack heldStack = held != null
                ? held.stack()
                : resolveHeldStackFor(player, activeInstrumentId);

        if (activeInstrumentId == null) {
            handleIdle(level, state, gameTime, player.getUUID());
            return;
        }

        // Broken-state gate: if the held stack's durability is 0, treat as idle
        // so the aura expires and the player isn't rewarded for a broken instrument.
        // IM's own sound pipeline is untouched — we don't try to block its audio.
        if (heldStack != null && InstrumentDurability.isBroken(heldStack)) {
            handleIdle(level, state, gameTime, player.getUUID());
            return;
        }

        AuraPreset aura = MobileInstrumentAuraMapping.resolveFor(
                player.getServer(), player.getUUID(), activeInstrumentId);
        if (aura == null) {
            handleIdle(level, state, gameTime, player.getUUID());
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

        if (aura.isOffensive() && !EIServerConfig.OFFENSIVE_AURAS_ENABLED.get()) {
            // Master kill switch — idle out instead of applying.
            handleIdle(level, state, gameTime, player.getUUID());
            return;
        }

        state.activeAura = aura;
        state.instrumentId = activeInstrumentId;
        state.lastActiveTick = gameTime;

        int radius = resolveMobileRadius(aura);
        int duration = aura.getEffectiveDuration();
        TargetingProfile profile = aura.isOffensive()
                ? TargetingProfiles.mobileOffensive()
                : TargetingProfiles.mobilePositive();
        AuraApplicator.apply(
                player,
                aura,
                radius,
                duration,
                profile,
                state.affectedTargets
        );

        // Visual feedback: spawn the aura-note particle plume so the player
        // can see the mobile aura is active. Matches the stationary tier's
        // post-apply particle call — users otherwise saw zero visual signal
        // that an IM instrument's aura was working.
        AuraManager.spawnAuraNotes(player, aura, radius);

        // Durability damage — one cost per successful pulse, polarity-aware.
        // Only charges when the player is actually holding the instrument (held
        // != null). Screen-open-only activation (e.g. browsing melody list in
        // the menu) doesn't consume durability — matches the user intuition
        // that durability tracks play, not browsing.
        if (held != null && heldStack != null && EIServerConfig.DURABILITY_ENABLED.get()) {
            int cost = EIServerConfig.DURABILITY_COST_PER_MOBILE_PULSE.get();
            if (aura.isOffensive()) {
                cost *= EIServerConfig.OFFENSIVE_DURABILITY_COST_MULT.get();
            }
            boolean brokeNow = InstrumentDurability.damage(heldStack, cost, player);
            if (brokeNow) {
                clearImmediate(level, state);
            }
        }
    }

    /**
     * Screen-open-only path helper: find the matching held stack for the
     * instrument whose IM screen is open. Used for the broken-state gate and
     * (not currently) durability decrements. Returns null if the instrument
     * isn't in either hand.
     */
    @Nullable
    private static net.minecraft.world.item.ItemStack resolveHeldStackFor(
            ServerPlayer player, @Nullable ResourceLocation instrumentId
    ) {
        if (instrumentId == null) return null;
        for (net.minecraft.world.item.ItemStack stack : player.getHandSlots()) {
            if (stack.isEmpty()) continue;
            net.minecraft.resources.ResourceLocation itemId =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (instrumentId.equals(itemId)) return stack;
        }
        return null;
    }

    /**
     * Player has no playing IM stack this pulse. Start (or continue) the linger
     * countdown; once it expires, strip effects from tracked targets and drop
     * them from the active-musician set so the per-tick scan stops hitting
     * them. They'll be re-added by {@link #onScreenOpened} or the discovery
     * scan if they pick the instrument back up.
     */
    private static void handleIdle(ServerLevel level, MobileAuraState state, long gameTime, java.util.UUID playerId) {
        if (state.activeAura == null) {
            // No active aura AND no held playing stack AND no screen open
            // (caller's invariant when calling this): drop the player from the
            // active set after the linger window so the cheap-path scan
            // doesn't churn on idle players.
            int linger = EIServerConfig.MOBILE_LINGER_TICKS.get();
            if (state.screenOpenInstrumentId == null
                    && gameTime - state.lastActiveTick >= linger) {
                ACTIVE_MOBILE_MUSICIANS.remove(playerId);
            }
            return;
        }

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
        if (state.screenOpenInstrumentId == null) {
            ACTIVE_MOBILE_MUSICIANS.remove(playerId);
        }
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

    // --- Lifecycle hooks (wired from InstrumentStateHandler + InstrumentOpenC2SPacket) ---

    /**
     * Client reported that an IM screen opened. Records the instrument id on
     * the mobile state; the next tick applies the aura even without
     * {@code playing=true} on the stack. This is how free-play mode gets aura
     * coverage — the user's "click the picker, nothing happens" complaint was
     * exactly this path being missing.
     */
    public static void onScreenOpened(ServerPlayer player, ResourceLocation instrumentId) {
        MobileAuraState state = STATES.computeIfAbsent(player.getUUID(), id -> new MobileAuraState());
        state.screenOpenInstrumentId = instrumentId;
        // Nudge lastActiveTick so a just-opened screen doesn't immediately go idle.
        state.lastActiveTick = player.level().getGameTime();
        // 1.4.9 (RECS §2.6): activate this player for the cheap-path tick scan.
        ACTIVE_MOBILE_MUSICIANS.add(player.getUUID());
    }

    /** Client reported that the IM screen closed — drop the screen-open flag. */
    public static void onScreenClosed(ServerPlayer player, ResourceLocation instrumentId) {
        MobileAuraState state = STATES.get(player.getUUID());
        if (state == null) return;
        if (instrumentId.equals(state.screenOpenInstrumentId)) {
            state.screenOpenInstrumentId = null;
        }
        // If the player closed the screen and isn't holding a playing stack
        // either, drop them from the active set so the tick scan stops
        // hitting them. handleIdle() will strip residual effects after the
        // configured linger window.
        if (state.screenOpenInstrumentId == null
                && ImmersiveMelodiesCompat.findActivePlayingStack(player) == null) {
            ACTIVE_MOBILE_MUSICIANS.remove(player.getUUID());
        }
    }

    /** Drop all per-player state. Called on logout. No effect-strip needed — the player is gone. */
    public static void onPlayerLogout(UUID playerId) {
        STATES.remove(playerId);
        LAST_OPEN_PACKET_TICK.remove(playerId);
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

    // Mobile-tier profiles now live on TargetingProfiles — one source of truth
    // for category inclusion across tiers. The old mobile-specific config keys
    // (MOBILE_ALLOW_SELF_BUFF, MOBILE_INCLUDE_OTHER_PLAYERS, MOBILE_INCLUDE_TAMED_PETS)
    // are deprecated; see EIServerConfig comments.

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

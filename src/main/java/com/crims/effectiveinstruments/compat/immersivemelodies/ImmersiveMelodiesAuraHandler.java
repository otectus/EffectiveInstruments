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
            handleIdle(level, state, gameTime);
            return;
        }

        // Broken-state gate: if the held stack's durability is 0, treat as idle
        // so the aura expires and the player isn't rewarded for a broken instrument.
        // IM's own sound pipeline is untouched — we don't try to block its audio.
        if (heldStack != null && InstrumentDurability.isBroken(heldStack)) {
            handleIdle(level, state, gameTime);
            return;
        }

        AuraPreset aura = MobileInstrumentAuraMapping.resolveFor(
                player.getServer(), player.getUUID(), activeInstrumentId);
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

        if (aura.isOffensive() && !EIServerConfig.OFFENSIVE_AURAS_ENABLED.get()) {
            // Master kill switch — idle out instead of applying.
            handleIdle(level, state, gameTime);
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
    }

    /** Client reported that the IM screen closed — drop the screen-open flag. */
    public static void onScreenClosed(ServerPlayer player, ResourceLocation instrumentId) {
        MobileAuraState state = STATES.get(player.getUUID());
        if (state == null) return;
        if (instrumentId.equals(state.screenOpenInstrumentId)) {
            state.screenOpenInstrumentId = null;
        }
    }

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

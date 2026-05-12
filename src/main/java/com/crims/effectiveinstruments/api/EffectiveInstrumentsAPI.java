package com.crims.effectiveinstruments.api;

import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Stable integration surface for third-party instrument mods that want
 * Effective Instruments auras to react to their own playables without
 * relying on the Genshin Instruments event bus.
 *
 * <p>All methods must be called on the server thread. The companion
 * instrument-aura mapping file ({@code config/effective_instruments/instrument_auras.json})
 * still governs which auras are available per instrument id — register
 * your instrument ids there to get default-selection UX.
 *
 * <p>This package is the only supported extension point. Do not import
 * classes from {@code com.crims.effectiveinstruments.aura} or
 * {@code .network} directly from other mods; those are internal and may
 * change between releases.
 */
public final class EffectiveInstrumentsAPI {

    /**
     * Marks the player as having opened an instrument screen. This is the
     * authoritative "open" signal that permits aura activation — equivalent
     * to whatever instrument-open event your backend emits. Call this from
     * your mod's server-side instrument-open handler.
     */
    public static void notifyInstrumentOpen(ServerPlayer player) {
        AuraManager.onInstrumentOpen((Player) player);
    }

    /**
     * Annotates the player's current instrument id and triggers default-aura
     * auto-selection based on the mod's instrument mapping file. Safe to
     * call any time after {@link #notifyInstrumentOpen}. The authoritative
     * open flag is NOT changed by this call, so third-party mods cannot
     * use this to bypass the trust boundary.
     */
    public static void notifyInstrumentIdReceived(ServerPlayer player, ResourceLocation instrumentId) {
        AuraManager.onInstrumentIdReceived(player, instrumentId);
    }

    /**
     * Marks the player as having closed the instrument screen. Clears
     * aura selection and any tracked targets.
     */
    public static void notifyInstrumentClose(ServerPlayer player) {
        AuraManager.onInstrumentClose((Player) player);
    }

    /**
     * Registers a note played by the musician. Drives the sliding-window
     * activation-threshold check — auras only tick while this is being
     * called at sustained cadence.
     */
    public static void notifyNotePlayed(ServerPlayer player) {
        AuraManager.onNotePlayed(player);
    }

    // --- Mobile tier (1.3.0+, Immersive Melodies compat) ---

    /** True if the Immersive Melodies compatibility layer detected the mod at startup. */
    public static boolean isImmersiveMelodiesCompatActive() {
        return ImmersiveMelodiesCompat.isAvailable();
    }

    /**
     * Read-only snapshot of a player's mobile-tier state, or {@code null} if none.
     * Safe to call whether or not Immersive Melodies is installed — returns null
     * in the absent case. Intended for HUD addons, debugging tools, and telemetry;
     * do not mutate state based on this return value.
     */
    @Nullable
    public static MobileState getMobileState(ServerPlayer player) {
        ImmersiveMelodiesAuraHandler.MobileStateView view =
                ImmersiveMelodiesAuraHandler.getView(player.getUUID());
        if (view == null) return null;
        return new MobileState(view.instrumentId(), view.auraId(), view.affectedTargetCount());
    }

    /** Immutable snapshot returned by {@link #getMobileState}. */
    public record MobileState(
            @Nullable ResourceLocation instrumentId,
            String auraId,
            int affectedTargetCount
    ) {}

    private EffectiveInstrumentsAPI() {}
}

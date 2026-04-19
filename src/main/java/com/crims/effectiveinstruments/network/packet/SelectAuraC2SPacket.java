package com.crims.effectiveinstruments.network.packet;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.aura.BuffTier;
import com.crims.effectiveinstruments.aura.InstrumentAuraMapping;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.aura.MobilePlayerSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Client → server: pick an aura. Two routing modes:
 * <ul>
 *   <li>{@code mobileInstrumentId} is empty → stationary mode; the selection
 *       is recorded in {@link AuraManager} and validated against the
 *       player's currently-open instrument (pre-1.4.1 behavior).</li>
 *   <li>{@code mobileInstrumentId} is non-empty → mobile mode; the selection
 *       is recorded in {@link MobilePlayerSelection} and validated against
 *       the {@link MobileInstrumentAuraMapping} allow-list for that
 *       instrument id.</li>
 * </ul>
 *
 * <p>Wire format bumped to protocol v4 in 1.4.1 — v1.4.0 clients cannot send
 * mobile selections, but the extra field is length-prefixed so decoder-side
 * forward-compat is straightforward if ever needed.
 */
public class SelectAuraC2SPacket {
    private final String auraId; // empty string = deselect
    private final String mobileInstrumentId; // empty = stationary mode

    public SelectAuraC2SPacket(String auraId) {
        this(auraId, "");
    }

    public SelectAuraC2SPacket(String auraId, String mobileInstrumentId) {
        this.auraId = auraId;
        this.mobileInstrumentId = mobileInstrumentId == null ? "" : mobileInstrumentId;
    }

    public static void encode(SelectAuraC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.auraId, 256);
        buf.writeUtf(msg.mobileInstrumentId, 256);
    }

    public static SelectAuraC2SPacket decode(FriendlyByteBuf buf) {
        String aura = buf.readUtf(256);
        String instrument = buf.readableBytes() > 0 ? buf.readUtf(256) : "";
        return new SelectAuraC2SPacket(aura, instrument);
    }

    public static void handle(SelectAuraC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            if (!msg.mobileInstrumentId.isEmpty()) {
                handleMobile(sender, msg);
            } else {
                handleStationary(sender, msg);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleStationary(ServerPlayer sender, SelectAuraC2SPacket msg) {
        // Rate limit: 5 tick cooldown (~250ms) between selections
        AuraManager.PlayerAuraState state = AuraManager.getState(sender.getUUID());
        if (state != null) {
            long now = sender.level().getGameTime();
            if (now - state.getLastSelectionTick() < 5) return;
            state.markSelectionTime(now);
        }

        if (msg.auraId.isEmpty()) {
            AuraManager.onAuraSwitch(sender);
            AuraManager.clearAuraSelection(sender);
            return;
        }

        Optional<AuraPreset> preset = AuraRegistry.getById(msg.auraId);
        if (preset.isEmpty() || !preset.get().enabled()) {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "Player {} tried to select invalid/disabled aura: {}",
                    sender.getName().getString(), msg.auraId);
            return;
        }

        if (state != null && state.getCurrentInstrumentId() != null) {
            InstrumentAuraMapping.InstrumentAuraConfig config =
                    InstrumentAuraMapping.getConfig(state.getCurrentInstrumentId());
            if (config != null && !config.allowedAuraIds().contains(msg.auraId)) {
                // Legacy-install recovery: a pre-1.4.0 mapping won't have offensive
                // counterparts in its allowed list. If the selected aura is the
                // counterpart of the instrument's default (positive/offensive
                // polarity inversion), accept the selection and log a one-shot
                // warning — the 1.4.x migration pass will regenerate the file
                // on next boot.
                if (!isCounterpartOfDefault(config, preset.get())) {
                    EffectiveInstrumentsMod.LOGGER.debug(
                            "Player {} tried to select aura '{}' not allowed for instrument '{}'",
                            sender.getName().getString(), msg.auraId, state.getCurrentInstrumentId());
                    return;
                }
                EffectiveInstrumentsMod.LOGGER.info(
                        "Accepting polarity-counterpart aura '{}' for instrument '{}' (legacy mapping without offensive entry)",
                        msg.auraId, state.getCurrentInstrumentId());
            }
        }

        AuraManager.onAuraSwitch(sender);
        AuraManager.setAuraSelection(sender, preset.get());
    }

    /**
     * True when {@code candidate} is the exact shipped polarity counterpart of
     * the instrument's default aura — i.e. the pair (default, candidate) is
     * registered in {@link InstrumentAuraMapping#isKnownPolarityPair}. Used as
     * a narrow acceptance escape hatch when a legacy (pre-1.4.x) mapping file
     * hasn't been migrated to include both polarities in its allowed list.
     *
     * <p>Strict by design: a server with custom high-amplifier offensive
     * presets can't be bypassed by sending an arbitrary offensive id — only
     * the shipped pair for that specific default is accepted.
     */
    private static boolean isCounterpartOfDefault(
            InstrumentAuraMapping.InstrumentAuraConfig config, AuraPreset candidate
    ) {
        Optional<AuraPreset> def = AuraRegistry.getById(config.defaultAuraId());
        if (def.isEmpty()) return false;
        if (def.get().isOffensive() == candidate.isOffensive()) return false;
        // Work out which id is positive and which is offensive, then verify
        // the shipped pair table lists them together.
        String positiveId = def.get().isOffensive() ? candidate.id() : def.get().id();
        String offensiveId = def.get().isOffensive() ? def.get().id() : candidate.id();
        return InstrumentAuraMapping.isKnownPolarityPair(positiveId, offensiveId);
    }

    private static void handleMobile(ServerPlayer sender, SelectAuraC2SPacket msg) {
        if (!ResourceLocation.isValidResourceLocation(msg.mobileInstrumentId)) {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "Player {} sent malformed mobile instrument id '{}'",
                    sender.getName().getString(), msg.mobileInstrumentId);
            return;
        }
        ResourceLocation instrumentId = new ResourceLocation(msg.mobileInstrumentId);

        MinecraftServer server = sender.getServer();
        if (server == null) return;
        MobilePlayerSelection selections = MobilePlayerSelection.get(server);

        if (msg.auraId.isEmpty()) {
            selections.setSelection(sender.getUUID(), instrumentId, null);
            return;
        }

        // Validate the aura exists, is enabled, supports mobile tier, and is in the allow-list.
        Optional<AuraPreset> preset = AuraRegistry.getById(msg.auraId);
        if (preset.isEmpty() || !preset.get().enabled() || !preset.get().supports(BuffTier.MOBILE)) {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "Player {} tried to select invalid mobile aura '{}'",
                    sender.getName().getString(), msg.auraId);
            return;
        }
        MobileInstrumentAuraMapping.MobileAuraConfig mapping =
                MobileInstrumentAuraMapping.getConfig(instrumentId);
        if (mapping == null || !mapping.allowedAuraIds().contains(msg.auraId)) {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "Player {} tried to select mobile aura '{}' not allowed for instrument '{}'",
                    sender.getName().getString(), msg.auraId, instrumentId);
            return;
        }

        selections.setSelection(sender.getUUID(), instrumentId, msg.auraId);
    }
}

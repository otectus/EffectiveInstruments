package com.crims.effectiveinstruments.performer;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraPreset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Default adapter for {@link ServerPlayer}. The 1.5.0 player path is preserved
 * byte-for-byte by routing every previously-player-only call site through
 * this wrapper: any change must be observable in both
 * {@code AuraBehaviorParityTest} and integration tests.
 *
 * <p>Two key behaviors:
 * <ul>
 *   <li>{@link #instrumentStack()} branches on tier — main hand for stationary,
 *       {@link MobileInstrumentLookup} for mobile.</li>
 *   <li>{@link #canPerformNow(ServerLevel)} mirrors {@code AuraManager.isActive}
 *       for stationary and delegates to the IM handler's state for mobile.</li>
 * </ul>
 */
public final class PlayerPerformer implements IAuraPerformer {

    private final ServerPlayer player;
    private final PerformerTier tier;

    public PlayerPerformer(ServerPlayer player, PerformerTier tier) {
        this.player = Objects.requireNonNull(player, "player");
        this.tier = Objects.requireNonNull(tier, "tier");
    }

    @Override public LivingEntity entity() { return player; }
    @Override public boolean isPlayer() { return true; }
    @Override public Optional<UUID> ownerUuid() { return Optional.of(player.getUUID()); }

    @Override public Optional<ServerPlayer> ownerIfOnline(MinecraftServer srv) {
        return Optional.of(player);
    }

    @Override public ItemStack instrumentStack() {
        if (tier == PerformerTier.MOBILE) {
            return MobileInstrumentLookup.find(player);
        }
        return player.getMainHandItem();
    }

    @Override public Optional<ResourceLocation> selectedAuraId() {
        AuraManager.PlayerAuraState state = AuraManager.getState(player.getUUID());
        if (state == null) return Optional.empty();
        AuraPreset preset = state.getSelectedAura();
        if (preset == null) return Optional.empty();
        // Preset ids are plain (no namespace separator) — namespace under EI explicitly.
        return Optional.of(new ResourceLocation(EffectiveInstrumentsMod.MODID, preset.id()));
    }

    @Override public PerformerTier tier() { return tier; }

    @Override public boolean canPerformNow(ServerLevel level) {
        return AuraManager.isActive(player.getUUID(), level.getGameTime());
    }

    /** Player swings are driven by vanilla input — adapter no-ops here. */
    @Override public void emitCue(ServerLevel lvl) { /* vanilla input handles swing */ }
}

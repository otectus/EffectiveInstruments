package com.crims.effectiveinstruments.compat.touhou_little_maid;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import com.crims.effectiveinstruments.performer.IAuraPerformer;
import com.crims.effectiveinstruments.performer.OwnerResolver;
import com.crims.effectiveinstruments.performer.PerformerTier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Touhou Little Maid {@link IAuraPerformer}. Owner via vanilla TamableAnimal
 * (caught by the global {@link OwnerResolver}). Combat veto goes through the
 * Maid's task system in addition to vanilla target/last-hurt checks.
 *
 * <p>{@link IAuraPerformer#emitCue} swings the mainhand — the maid's GeckoLib
 * controllers tolerate vanilla swing state, per spec §11 GeckoLib note (which
 * says swing is safe for maids if no custom animation controller is bound to
 * the EI synced byte). The synced-byte animation bridge is deferred to a
 * follow-up; the swing fallback is the documented degraded path.
 */
public final class MaidPerformer implements IAuraPerformer {

    private final LivingEntity maid;
    private final PerformerTier tier;

    public MaidPerformer(LivingEntity maid, PerformerTier tier) {
        this.maid = Objects.requireNonNull(maid);
        this.tier = Objects.requireNonNull(tier);
    }

    @Override public LivingEntity entity() { return maid; }
    @Override public Optional<UUID> ownerUuid() { return OwnerResolver.ownerOf(maid); }
    @Override public Optional<ServerPlayer> ownerIfOnline(MinecraftServer srv) {
        return ownerUuid().map(u -> srv.getPlayerList().getPlayer(u));
    }

    @Override public ItemStack instrumentStack() {
        ItemStack main = maid.getItemInHand(InteractionHand.MAIN_HAND);
        if (isPlayableInstrument(main)) return main;
        ItemStack off = maid.getItemInHand(InteractionHand.OFF_HAND);
        if (isPlayableInstrument(off)) return off;
        return ItemStack.EMPTY;
    }

    @Override public Optional<ResourceLocation> selectedAuraId() {
        AuraManager.PlayerAuraState state = AuraManager.getState(maid.getUUID());
        if (state == null) return Optional.empty();
        AuraPreset preset = state.getSelectedAura();
        if (preset == null) return Optional.empty();
        return Optional.of(new ResourceLocation(EffectiveInstrumentsMod.MODID, preset.id()));
    }

    @Override public PerformerTier tier() { return tier; }

    @Override public boolean canPerformNow(ServerLevel level) {
        if (!EIServerConfig.NPCS_ENABLED.get()) return false;
        if (!EIServerConfig.NPCS_ALLOW_PERFORMERS.get()) return false;
        if (TouhouMaidReflection.isInCombatState(maid)) return false;
        if (EIServerConfig.NPCS_REQUIRE_OWNER_ONLINE.get()) {
            MinecraftServer srv = level.getServer();
            if (srv == null) return false;
            if (ownerIfOnline(srv).isEmpty()) return false;
        }
        return true;
    }

    private boolean isPlayableInstrument(ItemStack stack) {
        if (stack.isEmpty() || InstrumentDurability.isBroken(stack)) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        if (tier == PerformerTier.MOBILE) return MobileInstrumentAuraMapping.hasMapping(id);
        String ns = id.getNamespace();
        return "genshinstrument".equals(ns) || "evenmoreinstruments".equals(ns);
    }
}

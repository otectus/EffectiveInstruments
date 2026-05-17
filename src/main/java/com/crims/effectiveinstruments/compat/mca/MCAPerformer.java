package com.crims.effectiveinstruments.compat.mca;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import com.crims.effectiveinstruments.performer.IAuraPerformer;
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
 * MCA Reborn villager performer adapter. 1.6.0 hotfix #5 promotion from
 * Tier-2 (target-only) to Tier-1 (full performer).
 *
 * <p>Owner: the villager's spouse UUID via {@link MCAReflection#spouseUuid}.
 * The performer is "owned by" the player she's married to — positive auras
 * from the spouse classify nearby allies as OWN_PET.
 *
 * <p>Combat veto: target acquired, recent damage, sleeping, trading (vanilla
 * checks). No MCA-specific bandit-trait detection in this pass.
 */
public final class MCAPerformer implements IAuraPerformer {

    private final LivingEntity villager;
    private final PerformerTier tier;

    public MCAPerformer(LivingEntity villager, PerformerTier tier) {
        this.villager = Objects.requireNonNull(villager);
        this.tier = Objects.requireNonNull(tier);
    }

    @Override public LivingEntity entity() { return villager; }

    @Override public Optional<UUID> ownerUuid() {
        return MCAReflection.spouseUuid(villager);
    }

    @Override public Optional<ServerPlayer> ownerIfOnline(MinecraftServer srv) {
        return ownerUuid().map(u -> srv.getPlayerList().getPlayer(u));
    }

    @Override public ItemStack instrumentStack() {
        ItemStack main = villager.getItemInHand(InteractionHand.MAIN_HAND);
        if (isPlayableInstrument(main)) return main;
        ItemStack off = villager.getItemInHand(InteractionHand.OFF_HAND);
        if (isPlayableInstrument(off)) return off;
        return ItemStack.EMPTY;
    }

    @Override public Optional<ResourceLocation> selectedAuraId() {
        AuraManager.PlayerAuraState state = AuraManager.getState(villager.getUUID());
        if (state == null) return Optional.empty();
        AuraPreset preset = state.getSelectedAura();
        if (preset == null) return Optional.empty();
        return Optional.of(new ResourceLocation(EffectiveInstrumentsMod.MODID, preset.id()));
    }

    @Override public PerformerTier tier() { return tier; }

    @Override public boolean canPerformNow(ServerLevel level) {
        if (!EIServerConfig.NPCS_ENABLED.get()) return false;
        if (!EIServerConfig.NPCS_ALLOW_PERFORMERS.get()) return false;
        if (MCAReflection.isInCombatState(villager)) return false;
        // Owner-online: when villager is unmarried (no spouse), the
        // requireOwnerOnline gate would always fail. Only enforce it when
        // we actually have a resolved owner — keeps un-spoused villagers
        // able to perform from the player's perspective.
        if (EIServerConfig.NPCS_REQUIRE_OWNER_ONLINE.get() && ownerUuid().isPresent()) {
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

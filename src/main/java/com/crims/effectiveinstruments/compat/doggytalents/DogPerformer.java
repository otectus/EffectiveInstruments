package com.crims.effectiveinstruments.compat.doggytalents;

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
 * Doggy Talents Next {@link IAuraPerformer}. Per spec §6.5: instrument lives
 * in the OFFHAND slot, gated by sitting-or-docile state. Owner via vanilla
 * {@code TamableAnimal} (caught by {@link OwnerResolver}'s short-circuit).
 *
 * <p>DTN has its own animation pipeline (not GeckoLib, not vanilla);
 * {@link IAuraPerformer#emitCue} swings the offhand which DTN's renderer
 * respects.
 */
public final class DogPerformer implements IAuraPerformer {

    private final LivingEntity dog;
    private final PerformerTier tier;

    public DogPerformer(LivingEntity dog, PerformerTier tier) {
        this.dog = Objects.requireNonNull(dog);
        this.tier = Objects.requireNonNull(tier);
    }

    @Override public LivingEntity entity() { return dog; }

    @Override public Optional<UUID> ownerUuid() { return OwnerResolver.ownerOf(dog); }

    @Override public Optional<ServerPlayer> ownerIfOnline(MinecraftServer srv) {
        return ownerUuid().map(u -> srv.getPlayerList().getPlayer(u));
    }

    @Override public ItemStack instrumentStack() {
        // OFFHAND preferred per spec §6.5; fall back to MAINHAND for users
        // who put the instrument in mainhand via creative-mode shenanigans.
        ItemStack off = dog.getItemInHand(InteractionHand.OFF_HAND);
        if (isPlayableInstrument(off)) return off;
        ItemStack main = dog.getItemInHand(InteractionHand.MAIN_HAND);
        if (isPlayableInstrument(main)) return main;
        return ItemStack.EMPTY;
    }

    @Override public InteractionHand instrumentHand() {
        // Visual swing uses whichever hand currently holds the instrument.
        ItemStack off = dog.getItemInHand(InteractionHand.OFF_HAND);
        return isPlayableInstrument(off) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override public Optional<ResourceLocation> selectedAuraId() {
        AuraManager.PlayerAuraState state = AuraManager.getState(dog.getUUID());
        if (state == null) return Optional.empty();
        AuraPreset preset = state.getSelectedAura();
        if (preset == null) return Optional.empty();
        return Optional.of(new ResourceLocation(EffectiveInstrumentsMod.MODID, preset.id()));
    }

    @Override public PerformerTier tier() { return tier; }

    @Override public boolean canPerformNow(ServerLevel level) {
        if (!EIServerConfig.NPCS_ENABLED.get()) return false;
        if (!EIServerConfig.NPCS_ALLOW_PERFORMERS.get()) return false;
        if (DoggyTalentsReflection.isInCombatState(dog)) return false;
        if (!DoggyTalentsReflection.isDocileOrSitting(dog)) return false;
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

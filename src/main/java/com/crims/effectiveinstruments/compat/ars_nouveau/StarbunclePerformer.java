package com.crims.effectiveinstruments.compat.ars_nouveau;

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
 * Starbuncle performer — ownerless. The starbuncle plays for any nearby
 * targets without an owner-based friend/foe split: friendly mobs in range
 * receive the positive aura, hostiles get debuffs from negative presets.
 *
 * <p>The {@code npcs.requireOwnerOnline} gate is implicitly bypassed for
 * starbuncles since there is no owner to be online; spec §15 open question
 * about ownerless performers is resolved here as "play if otherwise
 * eligible".
 */
public final class StarbunclePerformer implements IAuraPerformer {

    private final LivingEntity starby;
    private final PerformerTier tier;

    public StarbunclePerformer(LivingEntity starby, PerformerTier tier) {
        this.starby = Objects.requireNonNull(starby);
        this.tier = Objects.requireNonNull(tier);
    }

    @Override public LivingEntity entity() { return starby; }
    @Override public Optional<UUID> ownerUuid() { return Optional.empty(); }
    @Override public Optional<ServerPlayer> ownerIfOnline(MinecraftServer srv) { return Optional.empty(); }

    @Override public ItemStack instrumentStack() {
        ItemStack main = starby.getItemInHand(InteractionHand.MAIN_HAND);
        if (isPlayableInstrument(main)) return main;
        ItemStack off = starby.getItemInHand(InteractionHand.OFF_HAND);
        if (isPlayableInstrument(off)) return off;
        return ItemStack.EMPTY;
    }

    @Override public Optional<ResourceLocation> selectedAuraId() {
        AuraManager.PlayerAuraState state = AuraManager.getState(starby.getUUID());
        if (state == null) return Optional.empty();
        AuraPreset preset = state.getSelectedAura();
        if (preset == null) return Optional.empty();
        return Optional.of(new ResourceLocation(EffectiveInstrumentsMod.MODID, preset.id()));
    }

    @Override public PerformerTier tier() { return tier; }

    @Override public boolean canPerformNow(ServerLevel level) {
        if (!EIServerConfig.NPCS_ENABLED.get()) return false;
        if (!EIServerConfig.NPCS_ALLOW_PERFORMERS.get()) return false;
        if (ArsNouveauReflection.isInCombatState(starby)) return false;
        // Ownerless: skip requireOwnerOnline check entirely.
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

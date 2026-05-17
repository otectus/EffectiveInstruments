package com.crims.effectiveinstruments.compat.irons_spellbooks;

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
 * {@link IAuraPerformer} for Iron's Spells summons. Owner is the
 * {@link LivingEntity}'s UUID returned by {@code getSummoner()}.
 *
 * <p>Spec §6.6 mandates a "stop ~60 ticks before despawn" guard. Iron's
 * Spells uses a {@code SummonTimer} MobEffect to track lifetime — we read
 * that effect's remaining duration; if &lt; 60 ticks we stop performing so
 * the entity doesn't despawn mid-note. The effect resolution is best-effort
 * via the global effect registry (no compile dep on Iron's Spells).
 */
public final class SummonPerformer implements IAuraPerformer {

    private final LivingEntity summon;
    private final PerformerTier tier;

    public SummonPerformer(LivingEntity summon, PerformerTier tier) {
        this.summon = Objects.requireNonNull(summon);
        this.tier = Objects.requireNonNull(tier);
    }

    @Override public LivingEntity entity() { return summon; }

    @Override public Optional<UUID> ownerUuid() { return IronsSpellbooksReflection.ownerUuid(summon); }

    @Override public Optional<ServerPlayer> ownerIfOnline(MinecraftServer srv) {
        return ownerUuid().map(u -> srv.getPlayerList().getPlayer(u));
    }

    @Override public ItemStack instrumentStack() {
        ItemStack main = summon.getItemInHand(InteractionHand.MAIN_HAND);
        if (isPlayableInstrument(main)) return main;
        ItemStack off = summon.getItemInHand(InteractionHand.OFF_HAND);
        if (isPlayableInstrument(off)) return off;
        return ItemStack.EMPTY;
    }

    @Override public Optional<ResourceLocation> selectedAuraId() {
        AuraManager.PlayerAuraState state = AuraManager.getState(summon.getUUID());
        if (state == null) return Optional.empty();
        AuraPreset preset = state.getSelectedAura();
        if (preset == null) return Optional.empty();
        return Optional.of(new ResourceLocation(EffectiveInstrumentsMod.MODID, preset.id()));
    }

    @Override public PerformerTier tier() { return tier; }

    @Override public boolean canPerformNow(ServerLevel level) {
        if (!EIServerConfig.NPCS_ENABLED.get()) return false;
        if (!EIServerConfig.NPCS_ALLOW_PERFORMERS.get()) return false;
        if (IronsSpellbooksReflection.isInCombatState(summon)) return false;
        // Timer-bound: don't start a performance that won't finish.
        if (timerBelowSafetyMargin()) return false;
        if (EIServerConfig.NPCS_REQUIRE_OWNER_ONLINE.get()) {
            MinecraftServer srv = level.getServer();
            if (srv == null) return false;
            if (ownerIfOnline(srv).isEmpty()) return false;
        }
        return true;
    }

    /**
     * Iron's Spells summons carry a {@code summon_timer} or similar MobEffect.
     * We probe via the global effect registry for any effect on the summon
     * named like the summon-timer effect and check remaining duration.
     * Conservative: only veto when a clearly-summon-timer effect has &lt;60 ticks
     * remaining; if no effect found, allow play (treated as "no timer set").
     */
    private boolean timerBelowSafetyMargin() {
        // The effect registry name is mod-version-stable: "irons_spellbooks:summon_timer".
        ResourceLocation effectId = new ResourceLocation("irons_spellbooks", "summon_timer");
        net.minecraft.world.effect.MobEffect effect =
                ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) return false;
        net.minecraft.world.effect.MobEffectInstance inst = summon.getEffect(effect);
        if (inst == null) return false;
        return inst.getDuration() < 60;
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

package com.crims.effectiveinstruments.compat.recruits;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import com.crims.effectiveinstruments.performer.IAuraPerformer;
import com.crims.effectiveinstruments.performer.PerformerTier;
import com.crims.effectiveinstruments.performer.TargetingHint;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link IAuraPerformer} adapter for a Recruits soldier. State is keyed by
 * the recruit's UUID in the shared {@link AuraManager} state maps —
 * Phase 0's PerformerAuraState refactor made these maps entity-generic.
 *
 * <p>The instrument lookup walks the recruit's backpack inventory via
 * {@link RecruitsReflection#findInventoryStack} since recruits don't use
 * vanilla hand slots for their kit. Owner read is reflection-cached.
 */
public final class RecruitPerformer implements IAuraPerformer {

    private final LivingEntity recruit;
    private final PerformerTier tier;

    public RecruitPerformer(LivingEntity recruit, PerformerTier tier) {
        this.recruit = Objects.requireNonNull(recruit);
        this.tier = Objects.requireNonNull(tier);
    }

    @Override public LivingEntity entity() { return recruit; }

    @Override public Optional<UUID> ownerUuid() {
        return RecruitsReflection.ownerUuid(recruit);
    }

    @Override public Optional<ServerPlayer> ownerIfOnline(MinecraftServer srv) {
        return ownerUuid().map(u -> srv.getPlayerList().getPlayer(u));
    }

    @Override public ItemStack instrumentStack() {
        // Recruit inventories carry the instrument item. Filter by:
        // - tier match (stationary → GI/EMI namespaces, mobile → IM-mapped items)
        // - non-broken durability (a broken instrument is skipped so the goal
        //   doesn't keep firing on a zero-durability stack).
        return RecruitsReflection.findInventoryStack(recruit, stack -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) return false;
            String ns = id.getNamespace();
            if (InstrumentDurability.isBroken(stack)) return false;
            if (tier == PerformerTier.MOBILE) {
                return MobileInstrumentAuraMapping.hasMapping(id);
            }
            return "genshinstrument".equals(ns) || "evenmoreinstruments".equals(ns);
        });
    }

    @Override public Optional<ResourceLocation> selectedAuraId() {
        AuraManager.PlayerAuraState state = AuraManager.getState(recruit.getUUID());
        if (state == null) return Optional.empty();
        AuraPreset preset = state.getSelectedAura();
        if (preset == null) return Optional.empty();
        return Optional.of(new ResourceLocation(EffectiveInstrumentsMod.MODID, preset.id()));
    }

    @Override public PerformerTier tier() { return tier; }

    @Override public boolean canPerformNow(ServerLevel level) {
        if (!EIServerConfig.NPCS_ENABLED.get()) return false;
        if (!EIServerConfig.NPCS_ALLOW_PERFORMERS.get()) return false;
        if (!EIServerConfig.NPCS_RECRUITS_ALLOW_PERFORMERS.get()) return false;
        // Owner-online gate (server-wide default, applies to every NPC adapter).
        if (EIServerConfig.NPCS_REQUIRE_OWNER_ONLINE.get()) {
            MinecraftServer srv = level.getServer();
            if (srv == null) return false;
            if (ownerUuid().map(u -> srv.getPlayerList().getPlayer(u)).orElse(null) == null) {
                return false;
            }
        }
        // Combat-state veto — read through the cached reflection helper.
        if (RecruitsReflection.isInCombatState(recruit)) return false;
        return true;
    }

    @Override public TargetingHint classifyTarget(LivingEntity candidate) {
        // No Recruits-specific hints in Phase 2 — let the global classifier
        // (owner / team / faction) decide. RecruitFactionProvider already
        // contributes the ally signal via FactionResolver.
        return TargetingHint.DEFER;
    }

    @Override public void emitCue(ServerLevel lvl) {
        recruit.swing(instrumentHand());
    }
}

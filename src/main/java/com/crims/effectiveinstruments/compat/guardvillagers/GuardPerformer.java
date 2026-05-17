package com.crims.effectiveinstruments.compat.guardvillagers;

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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link IAuraPerformer} adapter for a Guard Villagers guard. Owner resolution
 * is two-stage per spec §6.2:
 * <ol>
 *   <li>{@code getOwnerId()} — if non-null, use it.</li>
 *   <li>Otherwise, search the nearby region for a player carrying
 *       {@link MobEffects#HERO_OF_THE_VILLAGE} — that player is the implicit
 *       owner. The search radius is {@code npcs.ownerNearbyRadius}.</li>
 * </ol>
 *
 * <p>No public inventory accessor on Guard, so instrument detection is
 * mainhand-only — matches a player's stationary path.
 */
public final class GuardPerformer implements IAuraPerformer {

    private final LivingEntity guard;
    private final PerformerTier tier;

    public GuardPerformer(LivingEntity guard, PerformerTier tier) {
        this.guard = Objects.requireNonNull(guard);
        this.tier = Objects.requireNonNull(tier);
    }

    @Override public LivingEntity entity() { return guard; }

    @Override public Optional<UUID> ownerUuid() {
        Optional<UUID> direct = GuardVillagersReflection.ownerUuid(guard);
        if (direct.isPresent()) return direct;
        // Hero-of-the-Village fallback per spec §6.2: ask a server-level call site to look up.
        // Cheap enough to do per-tick because the underlying query is a bounded AABB scan.
        if (guard.level() instanceof ServerLevel sl) {
            ServerPlayer hero = findNearbyHero(sl);
            if (hero != null) return Optional.of(hero.getUUID());
        }
        return Optional.empty();
    }

    @Override public Optional<ServerPlayer> ownerIfOnline(MinecraftServer srv) {
        return ownerUuid().map(u -> srv.getPlayerList().getPlayer(u));
    }

    @Override public ItemStack instrumentStack() {
        ItemStack main = guard.getItemInHand(InteractionHand.MAIN_HAND);
        if (isPlayableInstrument(main)) return main;
        ItemStack off = guard.getItemInHand(InteractionHand.OFF_HAND);
        if (isPlayableInstrument(off)) return off;
        return ItemStack.EMPTY;
    }

    @Override public Optional<ResourceLocation> selectedAuraId() {
        AuraManager.PlayerAuraState state = AuraManager.getState(guard.getUUID());
        if (state == null) return Optional.empty();
        AuraPreset preset = state.getSelectedAura();
        if (preset == null) return Optional.empty();
        return Optional.of(new ResourceLocation(EffectiveInstrumentsMod.MODID, preset.id()));
    }

    @Override public PerformerTier tier() { return tier; }

    @Override public boolean canPerformNow(ServerLevel level) {
        if (!EIServerConfig.NPCS_ENABLED.get()) return false;
        if (!EIServerConfig.NPCS_ALLOW_PERFORMERS.get()) return false;
        if (GuardVillagersReflection.isInCombatState(guard)) return false;
        if (EIServerConfig.NPCS_REQUIRE_OWNER_ONLINE.get()) {
            MinecraftServer srv = level.getServer();
            if (srv == null) return false;
            if (ownerIfOnline(srv).isEmpty()) return false;
        }
        return true;
    }

    private boolean isPlayableInstrument(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (InstrumentDurability.isBroken(stack)) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        if (tier == PerformerTier.MOBILE) {
            return MobileInstrumentAuraMapping.hasMapping(id);
        }
        String ns = id.getNamespace();
        return "genshinstrument".equals(ns) || "evenmoreinstruments".equals(ns);
    }

    @javax.annotation.Nullable
    private ServerPlayer findNearbyHero(ServerLevel sl) {
        double radius = EIServerConfig.NPCS_OWNER_NEARBY_RADIUS.get();
        double bestSq = radius * radius;
        ServerPlayer best = null;
        for (Player p : sl.players()) {
            if (!(p instanceof ServerPlayer sp)) continue;
            if (!sp.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) continue;
            double d = sp.distanceToSqr(guard);
            if (d <= bestSq) {
                bestSq = d;
                best = sp;
            }
        }
        return best;
    }
}

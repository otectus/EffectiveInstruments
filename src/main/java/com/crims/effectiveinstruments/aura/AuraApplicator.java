package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.config.EIServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tier-agnostic aura application and cleanup. Both the stationary tier
 * ({@link AuraManager}) and the mobile tier ({@code ImmersiveMelodiesAuraHandler})
 * delegate here so "strongest-wins" effect application, target gathering, and
 * tracked-target cleanup stay in one place.
 *
 * <p>All access occurs on the main server thread (tick handler, enqueueWork
 * packet handlers, Forge events), same as the former in-place implementation
 * on {@link AuraManager}.
 */
public final class AuraApplicator {

    // Cached pet allowlist shared by both tiers (same EIServerConfig.PET_ENTITY_ALLOWLIST
    // source). Invalidated by AuraRegistry.load() via invalidatePetAllowlistCache().
    private static Set<ResourceLocation> cachedPetAllowlist = Set.of();

    /**
     * Apply an aura's effects to all targets in range, updating {@code affectedTargets}
     * with the per-entity effect set so a later {@link #clear} call can strip only what
     * we applied. This is the exact behavior that used to live in
     * {@code AuraManager.applyAuraEffects}.
     *
     * @param source          the musician
     * @param aura            preset whose effects are applied
     * @param radius          pre-resolved radius in blocks (tiers resolve their own defaults)
     * @param durationTicks   pre-resolved effect duration in ticks
     * @param profile         targeting knobs (self/other-players/pets/cap)
     * @param affectedTargets per-entity-ID map of effects we applied, owned by the caller
     */
    public static void apply(
            ServerPlayer source,
            AuraPreset aura,
            int radius,
            int durationTicks,
            TargetingProfile profile,
            Map<Integer, Set<MobEffect>> affectedTargets
    ) {
        List<LivingEntity> targets = gatherTargets(source, radius, profile);
        for (LivingEntity target : targets) {
            Set<MobEffect> applied = affectedTargets
                    .computeIfAbsent(target.getId(), k -> new HashSet<>());
            for (AuraPreset.EffectEntry entry : aura.effects()) {
                if (applyEffectSafely(target, entry.effect(), entry.amplifier(), durationTicks)) {
                    applied.add(entry.effect());
                }
            }
        }
    }

    /**
     * Strip effects matching {@code oldAura} from all tracked targets.
     * Only removes effects that look like ours: ambient + matching amplifier +
     * remaining duration ≤ {@code maxExpectedDuration}. Stronger or longer effects
     * from other sources are preserved. This is the exact behavior that used to
     * live in {@code AuraManager.clearPreviousAuraEffects}.
     */
    public static void clear(
            ServerLevel level,
            @Nullable AuraPreset oldAura,
            int maxExpectedDuration,
            Map<Integer, Set<MobEffect>> affectedTargets
    ) {
        if (affectedTargets.isEmpty() || oldAura == null) return;

        // Build a map of effect -> amplifier for the old aura so we only strip ours
        Map<MobEffect, Integer> ourEffects = new HashMap<>();
        for (AuraPreset.EffectEntry entry : oldAura.effects()) {
            ourEffects.put(entry.effect(), entry.amplifier());
        }

        for (Map.Entry<Integer, Set<MobEffect>> entry : affectedTargets.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) continue;

            for (MobEffect effect : entry.getValue()) {
                Integer ourAmplifier = ourEffects.get(effect);
                if (ourAmplifier == null) continue;

                MobEffectInstance current = living.getEffect(effect);
                if (current == null) continue;

                // Only remove if the effect looks like ours: ambient + matching amplifier + plausible duration
                if (current.isAmbient() && current.getAmplifier() == ourAmplifier
                        && current.getDuration() <= maxExpectedDuration) {
                    living.removeEffect(effect);
                }
            }
        }

        affectedTargets.clear();
    }

    private static List<LivingEntity> gatherTargets(
            ServerPlayer source, int radius, TargetingProfile profile
    ) {
        int cap = profile.maxTargetsPerTick();
        List<LivingEntity> targets = new ArrayList<>();
        AABB box = source.getBoundingBox().inflate(radius);

        // Self (always first — guarantees the musician benefits even at low caps)
        if (profile.allowSelf()) {
            targets.add(source);
            if (targets.size() >= cap) return targets;
        }

        // Other players
        if (profile.includeOtherPlayers()) {
            for (Player other : source.serverLevel().getEntitiesOfClass(Player.class, box)) {
                if (other != source && source.distanceToSqr(other) <= (double) radius * radius) {
                    targets.add(other);
                    if (targets.size() >= cap) return targets;
                }
            }
        }

        // Tamed pets
        if (profile.includeTamedPets()) {
            Set<ResourceLocation> extraPetTypes = cachedPetAllowlist;

            for (Entity entity : source.serverLevel().getEntities(source, box)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (source.distanceToSqr(entity) > (double) radius * radius) continue;

                // TamableAnimal (wolf, cat, parrot, etc.)
                if (entity instanceof TamableAnimal tamable
                        && tamable.isTame()
                        && source.getUUID().equals(tamable.getOwnerUUID())) {
                    targets.add(living);
                    if (targets.size() >= cap) return targets;
                    continue;
                }

                // AbstractHorse (horse, donkey, mule, llama)
                if (entity instanceof AbstractHorse horse
                        && horse.isTamed()
                        && source.getUUID().equals(horse.getOwnerUUID())) {
                    targets.add(living);
                    if (targets.size() >= cap) return targets;
                    continue;
                }

                // Extra pet types from config
                ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                if (typeId != null && extraPetTypes.contains(typeId)) {
                    targets.add(living);
                    if (targets.size() >= cap) return targets;
                }
            }
        }

        return targets;
    }

    /**
     * Applies an aura effect under the configured
     * {@link com.crims.effectiveinstruments.config.OverwritePolicy}. Never removes
     * effects we didn't apply; cleanup is the job of {@link #clear}.
     *
     * @return true if the effect was actually applied
     */
    static boolean applyEffectSafely(
            LivingEntity target, MobEffect effect, int amplifier, int duration
    ) {
        MobEffectInstance existing = target.getEffect(effect);
        com.crims.effectiveinstruments.config.OverwritePolicy policy =
                EIServerConfig.EFFECT_OVERWRITE_POLICY.get();
        if (existing != null && !policy.shouldOverwrite(existing.getAmplifier(), amplifier)) {
            return false;
        }
        target.addEffect(new MobEffectInstance(
                effect, duration, amplifier,
                true,   // ambient (subtle particles)
                true,   // visible
                true    // show icon
        ));
        return true;
    }

    /** Refresh the cached pet-type allowlist from config. Called by AuraRegistry.load(). */
    public static void invalidatePetAllowlistCache() {
        cachedPetAllowlist = EIServerConfig.PET_ENTITY_ALLOWLIST.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    private AuraApplicator() {}
}

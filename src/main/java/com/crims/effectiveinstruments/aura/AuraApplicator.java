package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.performer.IAuraPerformer;
import com.crims.effectiveinstruments.performer.PerformerTier;
import com.crims.effectiveinstruments.performer.PlayerPerformer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
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
     * we applied.
     *
     * @param source          the musician
     * @param aura            preset whose effects are applied
     * @param radius          pre-resolved radius in blocks (tiers resolve their own defaults)
     * @param durationTicks   pre-resolved effect duration in ticks
     * @param profile         per-category inclusion + cap + polarity flag
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
        // 1.6.0: delegate to the IAuraPerformer-aware overload. The player path
        // is preserved byte-for-byte by routing through PlayerPerformer here.
        // Tier is STATIONARY because callers that want the mobile tier wire
        // ImmersiveMelodiesAuraHandler directly (which now also wraps).
        apply(new PlayerPerformer(source, PerformerTier.STATIONARY),
                aura, radius, durationTicks, profile, affectedTargets);
    }

    /**
     * 1.6.0: generic performer-aware aura application. Any {@link IAuraPerformer}
     * — player or NPC adapter — can drive this entry point. The legacy
     * {@code apply(ServerPlayer, ...)} overload is preserved as a wrapper.
     *
     * <p>For non-player performers the effective radius is scaled by
     * {@code npcs.performerAuraRadiusMultiplier} and any registered
     * {@link com.crims.effectiveinstruments.performer.PerformerRadiusModifier}
     * (e.g., Pehkui scale). The player path is never touched — 1.5.0 parity.
     */
    public static void apply(
            IAuraPerformer performer,
            AuraPreset aura,
            int radius,
            int durationTicks,
            TargetingProfile profile,
            Map<Integer, Set<MobEffect>> affectedTargets
    ) {
        int effectiveRadius = computeEffectiveRadius(performer, radius);
        List<LivingEntity> targets = gatherTargets(performer, effectiveRadius, profile);
        boolean offensive = aura.isOffensive();
        for (LivingEntity target : targets) {
            Set<MobEffect> applied = affectedTargets
                    .computeIfAbsent(target.getId(), k -> new HashSet<>());
            for (AuraPreset.EffectEntry entry : aura.effects()) {
                if (applyEffectSafely(target, entry.effect(), entry.amplifier(), durationTicks, offensive)) {
                    applied.add(entry.effect());
                }
            }
        }
    }

    /**
     * Strip effects matching {@code oldAura} from all tracked targets.
     * Only removes effects that look like ours: matching ambient polarity +
     * matching amplifier + remaining duration ≤ {@code maxExpectedDuration}.
     * The ambient check is polarity-aware — positive auras apply with
     * {@code ambient=true}, offensive auras with {@code ambient=false}, so
     * the strip-only-our-effects contract still holds on both sides. Stronger
     * or longer effects from other sources are preserved.
     */
    public static void clear(
            ServerLevel level,
            @Nullable AuraPreset oldAura,
            int maxExpectedDuration,
            Map<Integer, Set<MobEffect>> affectedTargets
    ) {
        if (affectedTargets.isEmpty() || oldAura == null) return;

        Map<MobEffect, Integer> ourEffects = new HashMap<>();
        for (AuraPreset.EffectEntry entry : oldAura.effects()) {
            ourEffects.put(entry.effect(), entry.amplifier());
        }
        boolean expectAmbient = !oldAura.isOffensive();

        for (Map.Entry<Integer, Set<MobEffect>> entry : affectedTargets.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) continue;

            for (MobEffect effect : entry.getValue()) {
                Integer ourAmplifier = ourEffects.get(effect);
                if (ourAmplifier == null) continue;

                MobEffectInstance current = living.getEffect(effect);
                if (current == null) continue;

                if (current.isAmbient() == expectAmbient
                        && current.getAmplifier() == ourAmplifier
                        && current.getDuration() <= maxExpectedDuration) {
                    living.removeEffect(effect);
                }
            }
        }

        affectedTargets.clear();
    }

    /**
     * Single-loop unified target gatherer. Walks every LivingEntity in the AABB
     * once, classifies each via {@link EntityCategory#classify}, and includes it
     * only if the profile's category set admits it. MUSICIAN and OWN_PET are
     * hard-wired by polarity (positive includes, offensive excludes) regardless
     * of the profile's category set — this is the contract the v1.4.1 design
     * promises the user.
     *
     * <p>Ordering: the targets list is built in category priority order
     * (musician → own pets → other players → other players' pets → villagers →
     * iron golems → passive → hostile) so a small cap still covers the
     * intuitively-important targets first. Inside each category, order is
     * whatever {@code level.getEntities} returns (effectively distance-ish).
     */
    private static List<LivingEntity> gatherTargets(
            IAuraPerformer performer, int radius, TargetingProfile profile
    ) {
        int cap = profile.maxTargetsPerTick();
        if (cap <= 0) return List.of();

        LivingEntity sourceEntity = performer.entity();
        if (!(sourceEntity.level() instanceof ServerLevel level)) return List.of();

        AABB box = sourceEntity.getBoundingBox().inflate(radius);
        double radiusSq = (double) radius * radius;

        // Bucket by category first so we can emit in priority order.
        EnumMap<EntityCategory, List<LivingEntity>> buckets = new EnumMap<>(EntityCategory.class);
        for (EntityCategory c : EntityCategory.values()) {
            buckets.put(c, new ArrayList<>());
        }

        // Source is always a candidate for MUSICIAN; ensure we don't skip it if
        // it sits outside level.getEntities(source, box) (it typically does
        // because the signature excludes the source entity).
        buckets.get(EntityCategory.MUSICIAN).add(sourceEntity);

        for (Entity entity : level.getEntities(sourceEntity, box)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (sourceEntity.distanceToSqr(entity) > radiusSq) continue;

            EntityCategory cat = EntityCategory.classify(performer, entity, cachedPetAllowlist);
            buckets.get(cat).add(living);
        }

        List<LivingEntity> out = new ArrayList<>();

        // MUSICIAN and OWN_PET: polarity-enforced, never config-gated.
        if (!profile.offensive()) {
            for (LivingEntity e : buckets.get(EntityCategory.MUSICIAN)) {
                out.add(e);
                if (out.size() >= cap) return out;
            }
            for (LivingEntity e : buckets.get(EntityCategory.OWN_PET)) {
                out.add(e);
                if (out.size() >= cap) return out;
            }
        }
        // Offensive path silently drops MUSICIAN + OWN_PET buckets.

        EntityCategory[] configGated = {
                EntityCategory.OTHER_PLAYER,
                EntityCategory.OTHER_PLAYER_PET,
                EntityCategory.VILLAGER,
                EntityCategory.IRON_GOLEM,
                EntityCategory.PASSIVE_MOB,
                EntityCategory.HOSTILE_MOB,
        };
        Set<EntityCategory> allowed = profile.allowedCategories();
        for (EntityCategory cat : configGated) {
            if (!allowed.contains(cat)) continue;
            for (LivingEntity e : buckets.get(cat)) {
                out.add(e);
                if (out.size() >= cap) return out;
            }
        }
        return out;
    }

    /**
     * Applies an aura effect under the configured
     * {@link com.crims.effectiveinstruments.config.OverwritePolicy}. Never removes
     * effects we didn't apply; cleanup is the job of {@link #clear}.
     *
     * <p>Positive auras use {@code ambient=true} so they produce subtle particles
     * on friendly targets. Offensive auras use {@code ambient=false} so hostile
     * mobs display normal (visible) particle plumes — users need a visual cue
     * that the debuff landed, otherwise offensive auras feel broken.
     *
     * @return true if the effect was actually applied
     */
    static boolean applyEffectSafely(
            LivingEntity target, MobEffect effect, int amplifier, int duration, boolean offensive
    ) {
        MobEffectInstance existing = target.getEffect(effect);
        com.crims.effectiveinstruments.config.OverwritePolicy policy =
                EIServerConfig.EFFECT_OVERWRITE_POLICY.get();
        if (existing != null && !policy.shouldOverwrite(existing.getAmplifier(), amplifier)) {
            return false;
        }
        target.addEffect(new MobEffectInstance(
                effect, duration, amplifier,
                !offensive, // ambient: true for positive (subtle), false for offensive (visible particles)
                true,       // visible
                true        // show icon
        ));
        return true;
    }

    /** Refresh the cached pet-type allowlist from config. Called by AuraRegistry.load(). */
    public static void invalidatePetAllowlistCache() {
        cachedPetAllowlist = EIServerConfig.PET_ENTITY_ALLOWLIST.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 1.6.0: non-player performers get their aura radius scaled by the global
     * {@code npcs.performerAuraRadiusMultiplier} (default 0.75) and any
     * registered {@link com.crims.effectiveinstruments.performer.PerformerRadiusModifier}
     * (Pehkui scale, future hooks). Player path is byte-identical to 1.5.0.
     * Result is clamped to [1, 64] — matching the existing radius parse range.
     */
    private static int computeEffectiveRadius(IAuraPerformer performer, int baseRadius) {
        if (performer.isPlayer()) return baseRadius;
        double mult;
        try {
            mult = EIServerConfig.NPCS_PERFORMER_AURA_RADIUS_MULTIPLIER.get();
        } catch (IllegalStateException ignored) {
            mult = 0.75; // SERVER config not loaded — use spec default
        }
        mult *= com.crims.effectiveinstruments.performer.PerformerRadiusModifier.Registry.applyAll(performer);
        int scaled = (int) Math.round(baseRadius * mult);
        return Math.max(1, Math.min(64, scaled));
    }

    private AuraApplicator() {}
}

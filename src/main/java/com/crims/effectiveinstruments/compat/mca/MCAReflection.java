package com.crims.effectiveinstruments.compat.mca;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * MCA Reborn reflection helper. The relationship oracle is the static
 * {@code EntityRelationship.of(Entity)} method returning
 * {@code Optional<EntityRelationship>} (verified vs 1.20.1 branch).
 *
 * <p>For Phase 4 we treat the spouse UUID as the "owner" so a player's
 * positive aura applies to their married MCA villager via the standard
 * owner-match path in {@link com.crims.effectiveinstruments.performer.OwnerResolver}.
 * The MCA villager has no inherent owner UUID — the marriage relationship is
 * the relationship oracle.
 */
final class MCAReflection {

    // 1.6.0 hotfix: MCA Reborn ships as an Architectury universal jar where
    // forge/, fabric/, quilt/ class-prefixes are PRESERVED at runtime, not
    // stripped (verified from `defineId called for: class forge.net.mca.entity.*`
    // log lines). Probe both prefixed and bare forms for forward-compat.
    static final String[] VILLAGER_MCA_FQN_CANDIDATES = {
            "forge.net.mca.entity.VillagerEntityMCA",
            "net.mca.entity.VillagerEntityMCA"
    };
    static final String[] ZOMBIE_VILLAGER_MCA_FQN_CANDIDATES = {
            "forge.net.mca.entity.ZombieVillagerEntityMCA",
            "net.mca.entity.ZombieVillagerEntityMCA"
    };
    static final String[] ENTITY_RELATIONSHIP_FQN_CANDIDATES = {
            "forge.net.mca.entity.ai.relationship.EntityRelationship",
            "net.mca.entity.ai.relationship.EntityRelationship"
    };

    @Nullable private static volatile Class<?> villagerClass;
    @Nullable private static volatile Class<?> zombieClass;
    @Nullable private static volatile Class<?> relationshipClass;
    @Nullable private static volatile java.lang.reflect.Method relationshipOfMethod;
    @Nullable private static volatile java.lang.reflect.Method getPartnerUuidMethod;
    private static volatile boolean resolved = false;

    static synchronized boolean tryResolve() {
        if (resolved) return villagerClass != null;
        resolved = true;
        villagerClass = firstLoadable(VILLAGER_MCA_FQN_CANDIDATES);
        if (villagerClass == null) return false;
        zombieClass = firstLoadable(ZOMBIE_VILLAGER_MCA_FQN_CANDIDATES);
        relationshipClass = firstLoadable(ENTITY_RELATIONSHIP_FQN_CANDIDATES);
        if (relationshipClass != null) {
            try {
                relationshipOfMethod = relationshipClass.getDeclaredMethod("of",
                        Class.forName("net.minecraft.world.entity.Entity"));
                relationshipOfMethod.setAccessible(true);
                getPartnerUuidMethod = relationshipClass.getMethod("getPartnerUUID");
                getPartnerUuidMethod.setAccessible(true);
            } catch (Throwable t) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "EI MCA adapter: EntityRelationship method lookup failed: {}", t.getMessage());
            }
        }
        EffectiveInstrumentsMod.LOGGER.info(
                "EI MCA adapter resolved: villager={} zombieVillager={} relationship={} partnerUuid={}",
                villagerClass != null, zombieClass != null,
                relationshipClass != null, getPartnerUuidMethod != null);
        return true;
    }

    @Nullable
    private static Class<?> firstLoadable(String[] candidates) {
        for (String fqn : candidates) {
            try { return Class.forName(fqn); }
            catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    static boolean isMcaVillager(LivingEntity e) {
        return (villagerClass != null && villagerClass.isInstance(e))
                || (zombieClass != null && zombieClass.isInstance(e));
    }

    /**
     * Spouse UUID per {@code EntityRelationship.of(villager).getPartnerUUID()}.
     * Returns empty for unmarried villagers, zombie villagers, or when MCA
     * isn't loaded.
     */
    /**
     * Combat-state veto for MCA villagers (Tier-1 promotion in 1.6.0 hotfix #5).
     * Honors target acquired, recent damage, sleeping (vanilla AbstractVillager),
     * trading (vanilla Villager). No MCA-specific reflection — bandit-trait
     * detection is deferred to follow-up.
     */
    static boolean isInCombatState(LivingEntity villager) {
        if (villager.getLastHurtByMob() != null) return true;
        if (villager instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.getTarget() != null) return true;
            if (mob.isAggressive()) return true;
        }
        if (villager instanceof net.minecraft.world.entity.npc.AbstractVillager av && av.isSleeping()) return true;
        if (villager instanceof net.minecraft.world.entity.npc.Villager v && v.isTrading()) return true;
        return false;
    }

    static Optional<UUID> spouseUuid(LivingEntity villager) {
        if (relationshipOfMethod == null || getPartnerUuidMethod == null) return Optional.empty();
        try {
            Object opt = relationshipOfMethod.invoke(null, villager); // static method
            if (!(opt instanceof Optional<?> o) || o.isEmpty()) return Optional.empty();
            Object rel = o.get();
            Object partnerOpt = getPartnerUuidMethod.invoke(rel);
            if (partnerOpt instanceof Optional<?> po && po.isPresent() && po.get() instanceof UUID u) {
                return Optional.of(u);
            }
            if (partnerOpt instanceof UUID u) return Optional.of(u);
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    private MCAReflection() {}
}

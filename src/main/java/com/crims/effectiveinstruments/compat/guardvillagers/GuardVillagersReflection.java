package com.crims.effectiveinstruments.compat.guardvillagers;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Reflection-cached gateway to the Guard Villagers API.
 * <ul>
 *   <li>{@code getOwnerId()} — returns {@link UUID} (verified via {@code javap}).</li>
 *   <li>{@code isFollowing()} / {@code isPatrolling()} — combat-state vetoes.</li>
 * </ul>
 *
 * <p>1.6.0 hotfix:
 * <ul>
 *   <li>Class FQN corrected from {@code tallestegg.guardvillagers.common.entities.Guard}
 *       to {@code tallestegg.guardvillagers.entities.Guard} (no {@code common.} segment).</li>
 *   <li>Switched from {@code MethodHandles.publicLookup()} to plain
 *       {@code Method.invoke()} for broader cross-mod compatibility.</li>
 * </ul>
 *
 * <p>No public inventory accessor exists on Guard (the {@code guardInventory}
 * field is private), so the instrument lookup uses the vanilla {@code MAINHAND}
 * slot — same as a player.
 */
final class GuardVillagersReflection {

    static final String GUARD_CLASS_FQN = "tallestegg.guardvillagers.entities.Guard";

    @Nullable private static volatile Class<?> guardClass;
    @Nullable private static volatile Method getOwnerIdMethod;
    @Nullable private static volatile Method isFollowingMethod;
    @Nullable private static volatile Method isPatrollingMethod;
    private static volatile boolean resolved = false;

    static synchronized boolean tryResolve() {
        if (resolved) return guardClass != null;
        resolved = true;
        try {
            guardClass = Class.forName(GUARD_CLASS_FQN);
        } catch (ClassNotFoundException e) {
            return false;
        }
        getOwnerIdMethod    = resolveMethod(guardClass, "getOwnerId");
        isFollowingMethod   = resolveMethod(guardClass, "isFollowing");
        isPatrollingMethod  = resolveMethod(guardClass, "isPatrolling");

        EffectiveInstrumentsMod.LOGGER.info(
                "EI Guard Villagers adapter resolved: owner={} follow={} patrol={}",
                getOwnerIdMethod != null, isFollowingMethod != null, isPatrollingMethod != null);
        return true;
    }

    static boolean isGuard(LivingEntity e) {
        return guardClass != null && guardClass.isInstance(e);
    }

    static Optional<UUID> ownerUuid(LivingEntity e) {
        if (getOwnerIdMethod == null) return Optional.empty();
        try {
            Object result = getOwnerIdMethod.invoke(e);
            if (result instanceof UUID u) return Optional.of(u);
            if (result instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof UUID u2) {
                return Optional.of(u2);
            }
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    /** Combat-state veto. Honors target, last-hurt, aggressive flag, patrolling. */
    static boolean isInCombatState(LivingEntity guard) {
        if (guard.getLastHurtByMob() != null) return true;
        if (guard instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.isAggressive()) return true;
            if (mob.getTarget() != null) return true;
        }
        return invokeBoolean(isPatrollingMethod, guard);
    }

    private static boolean invokeBoolean(@Nullable Method m, LivingEntity guard) {
        if (m == null) return false;
        try {
            Object r = m.invoke(guard);
            return r instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    @Nullable
    private static Method resolveMethod(Class<?> declaringClass, String name) {
        Class<?> c = declaringClass;
        while (c != null && c != Object.class) {
            try {
                Method m = c.getDeclaredMethod(name);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException nsme) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private GuardVillagersReflection() {}
}

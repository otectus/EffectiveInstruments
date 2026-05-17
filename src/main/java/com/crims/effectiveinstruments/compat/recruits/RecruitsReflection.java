package com.crims.effectiveinstruments.compat.recruits;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Reflection-cached gateway to the Recruits API. Every per-recruit field
 * (owner UUID, inventory, state, flee/rest flags) is read through a
 * {@link Method} resolved once at class-load and reused on every invocation.
 * Failures cache as no-ops so a Recruits API drift downgrades EI to a graceful
 * "no-recruit-performer" mode instead of a crash loop.
 *
 * <p>1.6.0 hotfix: switched from {@code MethodHandles.publicLookup()} to
 * {@code Method.invoke()} after the publicLookup pipeline silently failed
 * to resolve inherited / package-private accessors. {@code getAggroState}
 * does not exist in v1.15.0 — the state getter is {@code getState()} and the
 * setter is {@code setAggroState(int)} (asymmetric naming, verified via
 * {@code javap}). The AGGRO_RAID check was dropped: there is no public
 * constant exposing the int value of "raid state", and the existing combat
 * vetos (target acquired, recently hurt, fleeing flag, should-rest flag)
 * cover the spec §6.1 acceptance criteria without it.
 *
 * <p>Class FQN constants are deliberately strings rather than imports —
 * the {@code checkCompatAuditInvariant} gradle task enforces that
 * {@code compat.recruits.*} imports zero Recruits types.
 */
final class RecruitsReflection {

    static final String RECRUIT_CLASS_FQN = "com.talhanation.recruits.entities.AbstractRecruitEntity";

    @Nullable private static volatile Class<?> recruitClass;
    @Nullable private static volatile Method getOwnerUuidMethod;
    @Nullable private static volatile Method getInventoryMethod;
    @Nullable private static volatile Method isFollowingMethod;
    @Nullable private static volatile Method getFleeingMethod;
    @Nullable private static volatile Method getShouldRestMethod;
    private static volatile boolean resolved = false;

    static synchronized boolean tryResolve() {
        if (resolved) return recruitClass != null;
        resolved = true;
        try {
            recruitClass = Class.forName(RECRUIT_CLASS_FQN);
        } catch (ClassNotFoundException e) {
            return false;
        }
        getOwnerUuidMethod  = resolveMethod(recruitClass, "getOwnerUUID");
        getInventoryMethod  = resolveMethod(recruitClass, "getInventory");
        isFollowingMethod   = resolveMethod(recruitClass, "isFollowing");
        getFleeingMethod    = resolveMethod(recruitClass, "getFleeing");
        getShouldRestMethod = resolveMethod(recruitClass, "getShouldRest");

        EffectiveInstrumentsMod.LOGGER.info(
                "EI Recruits adapter resolved: owner={} inventory={} follow={} flee={} rest={}",
                getOwnerUuidMethod != null, getInventoryMethod != null,
                isFollowingMethod != null, getFleeingMethod != null,
                getShouldRestMethod != null);
        return true;
    }

    static boolean isRecruit(LivingEntity e) {
        return recruitClass != null && recruitClass.isInstance(e);
    }

    static Optional<UUID> ownerUuid(LivingEntity e) {
        if (getOwnerUuidMethod == null) return Optional.empty();
        try {
            Object result = getOwnerUuidMethod.invoke(e);
            if (result instanceof UUID u) return Optional.of(u);
            if (result instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof UUID u2) {
                return Optional.of(u2);
            }
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    /** Walk the recruit's backpack inventory for the first stack matching the predicate. */
    static ItemStack findInventoryStack(LivingEntity recruit, java.util.function.Predicate<ItemStack> predicate) {
        if (getInventoryMethod == null) return ItemStack.EMPTY;
        try {
            Object inv = getInventoryMethod.invoke(recruit);
            if (!(inv instanceof Container container)) return ItemStack.EMPTY;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && predicate.test(stack)) return stack;
            }
        } catch (Throwable ignored) {}
        return ItemStack.EMPTY;
    }

    /**
     * Combat veto. Honors target acquired, recent damage, fleeing flag, and
     * should-rest flag. AGGRO_RAID check was dropped in the 1.6.0 hotfix:
     * Recruits exposes no public getter for the state enum's int value, and
     * the target/hurt checks already cover combat states defensively.
     */
    static boolean isInCombatState(LivingEntity recruit) {
        if (recruit.getLastHurtByMob() != null) return true;
        if (recruit instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null) return true;
        if (invokeBoolean(getFleeingMethod, recruit)) return true;
        if (invokeBoolean(getShouldRestMethod, recruit)) return true;
        return false;
    }

    /** Best-effort: returns true when the recruit is in follow mode (owner trailing). */
    static boolean isFollowing(LivingEntity recruit) {
        return invokeBoolean(isFollowingMethod, recruit);
    }

    private static boolean invokeBoolean(@Nullable Method m, LivingEntity recruit) {
        if (m == null) return false;
        try {
            Object r = m.invoke(recruit);
            return r instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Find a zero-arg method by name on {@code declaringClass} or any
     * superclass, returning the {@link Method} with {@code setAccessible(true)}
     * applied. Returns null if no class in the hierarchy declares it.
     */
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

    private RecruitsReflection() {}
}

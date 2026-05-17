package com.crims.effectiveinstruments.compat.doggytalents;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Doggy Talents Next reflection helper. {@code Dog} extends
 * {@code doggytalents.api.inferface.AbstractDog} (note the typo in the package
 * name — that's how DTN ships) which extends {@code TamableAnimal}. Owner
 * UUID flows through the vanilla {@link com.crims.effectiveinstruments.performer.OwnerResolver}
 * path (TamableAnimal short-circuit).
 *
 * <p>Combat veto methods:
 * <ul>
 *   <li>{@code isInSittingPose()} — inherited from {@code TamableAnimal}.</li>
 *   <li>{@code getMode()} — declared on {@code Dog}; returns {@code DogMode}
 *       enum. Read as {@code Object} and compared via {@code toString()}.</li>
 * </ul>
 *
 * <p>1.6.0 hotfix: switched from {@code MethodHandles.publicLookup()} to
 * {@code Method.invoke()}. The publicLookup pipeline silently failed to
 * resolve {@code isInSittingPose} (inherited from {@code TamableAnimal})
 * even though the method is public — likely an asType receiver-narrowing
 * limitation. Plain reflection with {@code setAccessible(true)} works
 * unconditionally.
 */
final class DoggyTalentsReflection {

    static final String DOG_CLASS_FQN = "doggytalents.common.entity.Dog";

    @Nullable private static volatile Class<?> dogClass;
    @Nullable private static volatile Method getModeMethod;
    private static volatile boolean resolved = false;

    static synchronized boolean tryResolve() {
        if (resolved) return dogClass != null;
        resolved = true;
        try {
            dogClass = Class.forName(DOG_CLASS_FQN);
        } catch (ClassNotFoundException e) {
            return false;
        }
        // 1.6.0 hotfix #3: removed isInSittingPoseMethod reflection. The
        // method is declared on vanilla TamableAnimal; `getDeclaredMethod`
        // walks misfire on inherited vanilla methods under Forge's runtime
        // class transformer. Direct `instanceof TamableAnimal` bytecode
        // call works reliably (it's what EntityCategory + OwnerResolver
        // already do for isTame/getOwnerUUID).
        getModeMethod = resolveMethod(dogClass, "getMode");

        EffectiveInstrumentsMod.LOGGER.info(
                "EI Doggy Talents Next adapter resolved: mode={}",
                getModeMethod != null);
        return true;
    }

    static boolean isDog(LivingEntity e) {
        return dogClass != null && dogClass.isInstance(e);
    }

    /** True when the dog is in a state that should pause performance (combat, hostile mode). */
    static boolean isInCombatState(LivingEntity dog) {
        if (dog.getLastHurtByMob() != null) return true;
        if (dog instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.getTarget() != null) return true;
            if (mob.isAggressive()) return true;
        }
        String mode = readMode(dog);
        if (mode != null && (mode.contains("BERSERKER") || mode.contains("AGGRESSIVE"))) {
            return true;
        }
        return false;
    }

    /** True if dog is sitting OR in docile mode — eligible for performance. */
    static boolean isDocileOrSitting(LivingEntity dog) {
        // Sitting check: direct vanilla bytecode call via TamableAnimal.
        // Dog extends AbstractDog extends TamableAnimal so the instanceof
        // is always true for a Dog entity, but the check is defensive.
        if (dog instanceof TamableAnimal ta && ta.isInSittingPose()) return true;
        String mode = readMode(dog);
        return mode != null && mode.contains("DOCILE");
    }

    @Nullable
    private static String readMode(LivingEntity dog) {
        if (getModeMethod == null) return null;
        try {
            Object mode = getModeMethod.invoke(dog);
            return mode == null ? null : mode.toString();
        } catch (Throwable t) {
            return null;
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

    private DoggyTalentsReflection() {}
}

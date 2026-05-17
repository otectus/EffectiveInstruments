package com.crims.effectiveinstruments.compat.touhou_little_maid;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Touhou Little Maid reflection helper. The maid class is
 * {@code com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid}
 * which {@code extends TamableAnimal implements CrossbowAttackMob, IMaid}
 * (verified against the 1.20 branch).
 *
 * <p>The maid runs a Brain alongside her goalSelector — the task system uses
 * the Brain, but goalSelector still ticks. Goal injection works the same as
 * Phase 3 adapters; combat veto reads the current task via reflection on
 * {@code getTask()} and compares the task's {@code getUid()} against the
 * canonical "combat" / "attack" identifiers.
 *
 * <p>Owner UUID is read via vanilla {@code TamableAnimal.getOwnerUUID} —
 * the global {@link com.crims.effectiveinstruments.performer.OwnerResolver}
 * short-circuit catches this case, no provider needed.
 */
final class TouhouMaidReflection {

    static final String ENTITY_MAID_FQN = "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid";
    static final String IMAID_TASK_FQN  = "com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask";

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    @Nullable private static volatile Class<?> maidClass;
    @Nullable private static volatile Class<?> taskInterface;
    @Nullable private static volatile MethodHandle getTaskMH;
    @Nullable private static volatile MethodHandle taskGetUidMH;
    private static volatile boolean resolved = false;

    static synchronized boolean tryResolve() {
        if (resolved) return maidClass != null;
        resolved = true;
        try {
            maidClass = Class.forName(ENTITY_MAID_FQN);
        } catch (ClassNotFoundException e) {
            return false;
        }
        try {
            taskInterface = Class.forName(IMAID_TASK_FQN);
        } catch (ClassNotFoundException e) {
            taskInterface = null;
        }
        getTaskMH = unreflect("getTask", Object.class, maidClass);
        if (taskInterface != null) {
            try {
                java.lang.reflect.Method m = taskInterface.getMethod("getUid");
                m.setAccessible(true);
                taskGetUidMH = LOOKUP.unreflect(m).asType(MethodType.methodType(Object.class, Object.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                taskGetUidMH = null;
            }
        }
        EffectiveInstrumentsMod.LOGGER.info(
                "EI Touhou Little Maid adapter resolved: getTask={} taskGetUid={}",
                getTaskMH != null, taskGetUidMH != null);
        return true;
    }

    static boolean isMaid(LivingEntity e) {
        return maidClass != null && maidClass.isInstance(e);
    }

    /** Combat-state veto. Same shape as Phase 3 adapters plus a task-id check. */
    static boolean isInCombatState(LivingEntity maid) {
        if (maid.getLastHurtByMob() != null) return true;
        if (maid instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.getTarget() != null) return true;
            if (mob.isAggressive()) return true;
        }
        // Task-id veto: if the maid's current task UID contains "attack" or
        // "combat" we treat it as combat-mode and skip performance.
        String taskId = readTaskUid(maid);
        if (taskId != null) {
            String lower = taskId.toLowerCase();
            if (lower.contains("attack") || lower.contains("combat") || lower.contains("guard")) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static String readTaskUid(LivingEntity maid) {
        if (getTaskMH == null || taskGetUidMH == null) return null;
        try {
            Object task = getTaskMH.invoke(maid);
            if (task == null) return null;
            Object uid = taskGetUidMH.invoke(task);
            return uid == null ? null : uid.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    private static MethodHandle unreflect(String name, Class<?> returnType, Class<?> declaringClass) {
        Class<?> c = declaringClass;
        while (c != null && c != Object.class) {
            try {
                java.lang.reflect.Method m = c.getDeclaredMethod(name);
                if (!returnType.isAssignableFrom(m.getReturnType())) {
                    c = c.getSuperclass();
                    continue;
                }
                m.setAccessible(true);
                return LOOKUP.unreflect(m).asType(MethodType.methodType(returnType, LivingEntity.class));
            } catch (NoSuchMethodException nsme) {
                c = c.getSuperclass();
            } catch (IllegalAccessException iae) {
                return null;
            }
        }
        return null;
    }

    private TouhouMaidReflection() {}
}

package com.crims.effectiveinstruments.compat.ars_nouveau;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Ars Nouveau Starbuncle reflection helper. Confirmed via the {@code 1.20}
 * branch: Starbuncle extends {@code PathfinderMob} (not a TamableAnimal)
 * and does <b>not</b> track an owner UUID — only a "tamed" boolean. The
 * spec assumed otherwise; this adapter operates without owner classification,
 * so all targets fall through to the global TargetClassifier's vanilla bucket
 * (musician is the starbuncle itself; pets-of-musician are empty; everything
 * else is classified normally).
 *
 * <p>The "transport task" check (spec §6.7 acceptance: "starby with transport
 * job: never plays") would require deeper reflection into Starbuncle's
 * {@code dynamicBehavior} field. Phase 3 ships without that gate — the
 * starbuncle plays whenever it holds an instrument and is idle (no target,
 * not damaged).
 */
final class ArsNouveauReflection {

    static final String STARBUNCLE_CLASS_FQN = "com.hollingsworth.arsnouveau.common.entity.Starbuncle";

    @Nullable private static volatile Class<?> starbuncleClass;
    private static volatile boolean resolved = false;

    static synchronized boolean tryResolve() {
        if (resolved) return starbuncleClass != null;
        resolved = true;
        try {
            starbuncleClass = Class.forName(STARBUNCLE_CLASS_FQN);
        } catch (ClassNotFoundException e) {
            return false;
        }
        EffectiveInstrumentsMod.LOGGER.info("EI Ars Nouveau adapter resolved: Starbuncle class found");
        return true;
    }

    static boolean isStarbuncle(LivingEntity e) {
        return starbuncleClass != null && starbuncleClass.isInstance(e);
    }

    static boolean isInCombatState(LivingEntity starby) {
        if (starby.getLastHurtByMob() != null) return true;
        if (starby instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.getTarget() != null) return true;
        }
        return false;
    }

    private ArsNouveauReflection() {}
}

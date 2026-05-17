package com.crims.effectiveinstruments.compat.easy_npc;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Easy NPC ({@code de.markusbordihn.easynpc.entity.easynpc.EasyNPC}) marker-
 * interface gateway. The mod's NPC entities all implement the {@code EasyNPC}
 * generic interface (verified against the {@code 1.20.1} branch).
 *
 * <p>Owner resolution is delegated to the vanilla {@link
 * com.crims.effectiveinstruments.performer.OwnerResolver} path because
 * Easy NPC's {@code OwnerDataCapable} interface extends
 * {@code net.minecraft.world.entity.OwnableEntity} — caught by the short-
 * circuit at the top of {@code OwnerResolver.ownerOf}.
 */
final class EasyNpcReflection {

    static final String EASYNPC_INTERFACE_FQN = "de.markusbordihn.easynpc.entity.easynpc.EasyNPC";

    @Nullable private static volatile Class<?> easyNpcClass;
    private static volatile boolean resolved = false;

    static synchronized boolean tryResolve() {
        if (resolved) return easyNpcClass != null;
        resolved = true;
        try {
            easyNpcClass = Class.forName(EASYNPC_INTERFACE_FQN);
        } catch (ClassNotFoundException e) {
            return false;
        }
        EffectiveInstrumentsMod.LOGGER.info("EI Easy NPC adapter resolved: interface found");
        return true;
    }

    static boolean isEasyNpc(LivingEntity e) {
        return easyNpcClass != null && easyNpcClass.isInstance(e);
    }

    /** Easy NPC combat-state veto: target acquired, took damage, or aggressive flag set. */
    static boolean isInCombatState(LivingEntity npc) {
        if (npc.getLastHurtByMob() != null) return true;
        if (npc instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.isAggressive()) return true;
            if (mob.getTarget() != null) return true;
        }
        return false;
    }

    private EasyNpcReflection() {}
}

package com.crims.effectiveinstruments.compat.irons_spellbooks;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Iron's Spells 'n Spellbooks reflection helper.
 *
 * <p>1.6.0 hotfix: the real interface is
 * {@code io.redspace.ironsspellbooks.entity.mobs.IMagicSummon} (6535-byte file,
 * confirmed via {@code javap} of v3.15.6). The {@code MagicSummon} class
 * present in the same package is a 337-byte deprecated stub. {@code getSummoner()}
 * returns {@link Entity}, not {@link LivingEntity}.
 *
 * <p>Owner UUID is derived by checking {@code getSummoner() instanceof LivingEntity}
 * — the summoner could in principle be a non-living entity (a dispenser fragment,
 * a spawner), so we filter before reading the UUID.
 */
final class IronsSpellbooksReflection {

    static final String MAGIC_SUMMON_INTERFACE_FQN = "io.redspace.ironsspellbooks.entity.mobs.IMagicSummon";

    @Nullable private static volatile Class<?> magicSummonClass;
    @Nullable private static volatile Method getSummonerMethod;
    private static volatile boolean resolved = false;

    static synchronized boolean tryResolve() {
        if (resolved) return magicSummonClass != null;
        resolved = true;
        try {
            magicSummonClass = Class.forName(MAGIC_SUMMON_INTERFACE_FQN);
        } catch (ClassNotFoundException e) {
            return false;
        }
        try {
            getSummonerMethod = magicSummonClass.getMethod("getSummoner");
            getSummonerMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "EI Iron's Spells adapter: getSummoner method not found on {}",
                    MAGIC_SUMMON_INTERFACE_FQN);
        }
        EffectiveInstrumentsMod.LOGGER.info(
                "EI Iron's Spells adapter resolved: getSummoner={}", getSummonerMethod != null);
        return true;
    }

    static boolean isMagicSummon(LivingEntity e) {
        return magicSummonClass != null && magicSummonClass.isInstance(e);
    }

    static Optional<UUID> ownerUuid(LivingEntity summon) {
        if (getSummonerMethod == null) return Optional.empty();
        try {
            Object result = getSummonerMethod.invoke(summon);
            if (result instanceof LivingEntity le) return Optional.of(le.getUUID());
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    /** Iron's summons are timer-bound — combat veto is target/last-hurt only here. */
    static boolean isInCombatState(LivingEntity summon) {
        if (summon.getLastHurtByMob() != null) return true;
        if (summon instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.getTarget() != null) return true;
            if (mob.isAggressive()) return true;
        }
        return false;
    }

    private IronsSpellbooksReflection() {}
}

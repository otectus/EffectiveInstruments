package com.crims.effectiveinstruments.compat.recruits;

import com.crims.effectiveinstruments.performer.OwnerProvider;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Registers Recruits owner-UUID resolution with the global
 * {@link com.crims.effectiveinstruments.performer.OwnerResolver}. Used by:
 * <ul>
 *   <li>{@link com.crims.effectiveinstruments.performer.TargetClassifier} — to
 *       bucket recruits owned by the musician as OWN_PET.</li>
 *   <li>Other per-mod adapters that need to ask "is this entity owned by my
 *       owner?" without taking a hard dep on Recruits.</li>
 * </ul>
 */
public final class RecruitOwnerProvider implements OwnerProvider {
    @Override public boolean appliesTo(LivingEntity e) {
        return RecruitsReflection.isRecruit(e);
    }
    @Override public Optional<UUID> ownerOf(LivingEntity e) {
        return RecruitsReflection.ownerUuid(e);
    }
}

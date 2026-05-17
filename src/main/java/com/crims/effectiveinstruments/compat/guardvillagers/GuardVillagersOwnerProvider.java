package com.crims.effectiveinstruments.compat.guardvillagers;

import com.crims.effectiveinstruments.performer.OwnerProvider;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Owner provider for Guard Villagers. Per-source verification: Guard does
 * have a {@code getOwnerId()} method despite spec §6.2's claim that "Guards
 * have no UUID owner" — returns null for unowned guards. Falls through
 * cleanly when no owner is set.
 */
public final class GuardVillagersOwnerProvider implements OwnerProvider {
    @Override public boolean appliesTo(LivingEntity e) { return GuardVillagersReflection.isGuard(e); }
    @Override public Optional<UUID> ownerOf(LivingEntity e) { return GuardVillagersReflection.ownerUuid(e); }
}

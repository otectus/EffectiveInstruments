package com.crims.effectiveinstruments.performer;

import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Per-mod owner-UUID resolver. Adapters register an instance with
 * {@link OwnerResolver#register(OwnerProvider)} at bootstrap so the central
 * classifier can read owner-of-entity uniformly across mods. The vanilla
 * {@code OwnableEntity} short-circuit is built into {@code OwnerResolver}
 * and runs before any provider is queried.
 */
public interface OwnerProvider {
    /** Cheap pre-check: does this provider know about this entity type? */
    boolean appliesTo(LivingEntity e);

    /** Resolve the owner UUID. Return empty when unknown rather than throwing. */
    Optional<UUID> ownerOf(LivingEntity e);
}

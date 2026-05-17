package com.crims.effectiveinstruments.performer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Per-mod faction relationship oracle. Adapters register an instance with
 * {@link FactionResolver#register(FactionProvider)} at bootstrap.
 *
 * <p>The relationship answer matters more than the faction id: a Recruits
 * soldier on faction {@code RED} vs a player on faction {@code BLUE} resolves
 * to {@link FactionRelationship#ENEMY}; vs another soldier on {@code RED} it
 * is {@link FactionRelationship#ALLY}.
 */
public interface FactionProvider {
    /** Cheap pre-check: does this provider know about either entity? */
    boolean appliesTo(LivingEntity e);

    /** Faction id of {@code e}, if any. */
    Optional<ResourceLocation> factionOf(LivingEntity e);

    /** Relationship between two entities under this provider's faction system. */
    FactionRelationship relationship(LivingEntity a, LivingEntity b);
}

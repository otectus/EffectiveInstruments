package com.crims.effectiveinstruments.compat.recruits;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.performer.FactionProvider;
import com.crims.effectiveinstruments.performer.FactionRelationship;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Maps Recruits' diplomacy state into the cross-mod
 * {@link FactionRelationship} oracle. Phase 2 implementation conservatively
 * returns {@link FactionRelationship#UNKNOWN} so {@link com.crims.effectiveinstruments.performer.TargetClassifier}
 * falls through to the owner/team/vanilla buckets — adding faction-aware
 * routing (RecruitsDiplomacyManager probe) is a follow-up task once the
 * test fixtures verify the diplomacy data shape.
 *
 * <p>Until then: same-owner recruits are still classified ALLY via the
 * owner resolver path, and unowned/hostile-faction recruits fall to the
 * vanilla bucket (HOSTILE_MOB if Enemy / PASSIVE_MOB otherwise).
 */
public final class RecruitFactionProvider implements FactionProvider {

    @Override public boolean appliesTo(LivingEntity e) {
        return RecruitsReflection.isRecruit(e);
    }

    @Override public Optional<ResourceLocation> factionOf(LivingEntity e) {
        // Recruits' group UUID is per-recruit; surfacing it as a synthetic
        // ResourceLocation gives diagnostics something to display without
        // requiring a deep diplomacy probe.
        Optional<UUID> owner = RecruitsReflection.ownerUuid(e);
        return owner.map(u -> new ResourceLocation(EffectiveInstrumentsMod.MODID,
                "recruits/owner/" + u.toString().replace('-', '_')));
    }

    @Override public FactionRelationship relationship(LivingEntity a, LivingEntity b) {
        // Phase 2: same-owner → ALLY; cross-owner → UNKNOWN (TargetClassifier
        // falls through to owner/team buckets which already handle "different
        // owners means not-an-ally" via OTHER_PLAYER_PET).
        Optional<UUID> ownerA = RecruitsReflection.isRecruit(a)
                ? RecruitsReflection.ownerUuid(a) : Optional.empty();
        Optional<UUID> ownerB = RecruitsReflection.isRecruit(b)
                ? RecruitsReflection.ownerUuid(b) : Optional.empty();
        if (ownerA.isPresent() && ownerB.isPresent() && ownerA.get().equals(ownerB.get())) {
            return FactionRelationship.ALLY;
        }
        return FactionRelationship.UNKNOWN;
    }
}

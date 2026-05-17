package com.crims.effectiveinstruments.compat.mca;

import com.crims.effectiveinstruments.performer.OwnerProvider;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * MCA villager "owner" = spouse UUID per spec §6.8: positive auras from a
 * player apply to their married villager (treated as OWN_PET via the global
 * classifier's owner-match path). Unmarried villagers fall through to the
 * default VILLAGER bucket — the player's {@code positiveTargeting.includeVillagers}
 * toggle still governs whether positive auras reach them.
 */
public final class MCAOwnerProvider implements OwnerProvider {

    @Override public boolean appliesTo(LivingEntity e) {
        return MCAReflection.isMcaVillager(e);
    }

    @Override public Optional<UUID> ownerOf(LivingEntity e) {
        return MCAReflection.spouseUuid(e);
    }
}

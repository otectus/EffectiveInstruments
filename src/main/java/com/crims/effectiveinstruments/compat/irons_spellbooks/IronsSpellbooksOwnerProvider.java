package com.crims.effectiveinstruments.compat.irons_spellbooks;

import com.crims.effectiveinstruments.performer.OwnerProvider;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public final class IronsSpellbooksOwnerProvider implements OwnerProvider {
    @Override public boolean appliesTo(LivingEntity e) { return IronsSpellbooksReflection.isMagicSummon(e); }
    @Override public Optional<UUID> ownerOf(LivingEntity e) { return IronsSpellbooksReflection.ownerUuid(e); }
}

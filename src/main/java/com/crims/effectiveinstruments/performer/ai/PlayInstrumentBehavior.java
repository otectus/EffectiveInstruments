package com.crims.effectiveinstruments.performer.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * Reusable {@link Behavior} base for Tier-1 Brain-driven adapters (Touhou Maid
 * in Phase 4; MCA villagers deferred to 1.7.0). Mirrors the state machine in
 * {@link PlayInstrumentGoal} but expressed via memory preconditions —
 * {@code ATTACK_TARGET} absent, {@code WALK_TARGET} absent, plus an EI memory
 * module that marks the entity as instrument-holding (registered in Phase 4
 * via {@code EIMemories}).
 *
 * <p>Phase 0 ships this as a scaffold so call sites can reference it.
 * Concrete subclasses (e.g., {@code MaidInstrumentBehavior}) override
 * {@link #checkExtraStartConditions}, {@link #start}, {@link #canStillUse}.
 */
public class PlayInstrumentBehavior extends Behavior<LivingEntity> {

    public PlayInstrumentBehavior(Map<MemoryModuleType<?>, MemoryStatus> preconditions,
                                  int minDuration, int maxDuration) {
        super(preconditions, minDuration, maxDuration);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LivingEntity owner) {
        return false;
    }

    @Override
    protected void start(ServerLevel level, LivingEntity entity, long gameTime) {
        // Phase 4 subclasses fill this in.
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LivingEntity entity, long gameTime) {
        return false;
    }
}

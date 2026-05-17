package com.crims.effectiveinstruments.compat.recruits;

import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.config.EIServerConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge-bus event handlers for Recruits. Registered conditionally by
 * {@link RecruitsCompat#initCommon()} when Recruits is present at runtime.
 *
 * <ul>
 *   <li>{@link #onEntityJoinLevel} — injects {@link RecruitInstrumentGoal}
 *       into the recruit's {@code goalSelector} at priority 3 per spec §6.1.</li>
 *   <li>{@link #onLivingDeath} — cleans per-recruit state from the shared
 *       {@code AuraManager} state map and the IM mobile-NPC set.</li>
 * </ul>
 *
 * <p>Priority 3 sits between Recruits' own {@code RecruitFollowOwnerGoal}
 * (priority 2) and {@code RecruitMoveToPosGoal} (priority 3+). The
 * {@code MOVE}/{@code LOOK} mutex flags ensure combat goals preempt without
 * explicit cancellation.
 */
public final class RecruitsEventHandler {

    private static final int GOAL_PRIORITY = 3;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!EIServerConfig.NPCS_ENABLED.get()) return;
        if (!EIServerConfig.NPCS_RECRUITS_ENABLED.get()) return;

        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!RecruitsReflection.isRecruit(mob)) return;
        if (!EIServerConfig.NPCS_RECRUITS_ALLOW_PERFORMERS.get()) return;

        try {
            mob.goalSelector.addGoal(GOAL_PRIORITY, new RecruitInstrumentGoal(mob));
        } catch (Throwable t) {
            // Recruits' goalSelector is typically standard — if injection throws
            // we silently skip rather than crash the join event.
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!RecruitsReflection.isRecruit(e)) return;
        // Drop the mobile-tier NPC entry if any. AuraManager.PERFORMER_STATES
        // gets pruned amortized by the per-tick scan; explicit cleanup avoids
        // a one-minute lag window for the rare case the recruit dies mid-aura.
        ImmersiveMelodiesAuraHandler.unregisterActiveMobileNpc(e.getUUID());
    }

    /**
     * Belt-and-suspenders: drop the mobile NPC entry on dimension change so a
     * recruit teleported via Recruits' "command" UI doesn't keep a stale state.
     */
    @SubscribeEvent
    public static void onLivingChangeDimension(LivingEvent.LivingTickEvent event) {
        // No-op stub today — placeholder for the per-200-tick re-injection sweep
        // mentioned in spec §6.3 (Easy NPC profession-swap) which gets shared
        // across all Goal-based adapters. Phase 3+ may wire this when adding
        // Easy NPC.
    }

    private RecruitsEventHandler() {}
}

package com.crims.effectiveinstruments.performer.ai;

import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.aura.InstrumentAuraMapping;
import com.crims.effectiveinstruments.aura.Polarity;
import com.crims.effectiveinstruments.event.StationaryInstrumentNoteService;
import com.crims.effectiveinstruments.performer.IAuraPerformer;
import com.crims.effectiveinstruments.performer.PerformerRegistry;
import com.crims.effectiveinstruments.performer.PerformerTier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Reusable {@link Goal} base for Tier-1 goalSelector adapters (Recruits, Guard
 * Villagers, Easy NPC, Doggy Talents, Iron's Spells summons, Ars Nouveau
 * Starbuncle). Concrete subclasses constrain {@link #canUse()} with mod-
 * specific gates (target null, combat-state checks, follow-mode checks).
 *
 * <p>State machine per spec §10:
 * <pre>
 * INACTIVE -> READY (instrument present, owner online, no combat)
 * READY -> PLAYING_SUPPORT | PLAYING_OFFENSIVE (preset polarity drives the split)
 * PLAYING_* -> INTERRUPTED (target acquired, hurt, owner offline, dimension change)
 * PLAYING_* -> COOLDOWN (preset duration elapsed)
 * INTERRUPTED -> COOLDOWN (1.5s linger for visual fade)
 * COOLDOWN -> READY (cooldownTicks elapsed; canUse becomes true again)
 * </pre>
 *
 * <p>{@link #tick()} delegates note processing to
 * {@link StationaryInstrumentNoteService#processNote(IAuraPerformer, ItemStack)}
 * so durability, broken-state gate, and aura record share the player path.
 */
public class PlayInstrumentGoal extends Goal {

    public enum State {
        INACTIVE,
        READY,
        PLAYING_SUPPORT,
        PLAYING_OFFENSIVE,
        INTERRUPTED,
        COOLDOWN
    }

    protected final Mob host;
    protected State state = State.INACTIVE;
    protected int stateTicks;
    protected IAuraPerformer perf;
    protected int cooldownTicks = 60;

    public PlayInstrumentGoal(Mob host, EnumSet<Flag> mutex) {
        this.host = host;
        this.setFlags(mutex);
    }

    /** Sub-classes override for combat-state veto. Base requires no target + a wrappable performer. */
    @Override
    public boolean canUse() {
        if (host.getTarget() != null) return false;
        perf = PerformerRegistry.wrap(host, PerformerTier.STATIONARY).orElse(null);
        if (perf == null) return false;
        if (perf.instrumentStack().isEmpty()) return false;
        ServerLevel sl = (host.level() instanceof ServerLevel s) ? s : null;
        return sl != null && perf.canPerformNow(sl) && coolDownElapsed();
    }

    @Override
    public boolean canContinueToUse() {
        return state != State.COOLDOWN && state != State.INACTIVE;
    }

    @Override
    public void start() {
        transitionTo(State.READY);
        // 1.6.0 hotfix #5: register the NPC in IM's mobile-NPC tick set when
        // the held instrument has a mobile mapping. Without this the mobile
        // pulser never sees the NPC and IM-tier auras stay dormant. The
        // registration set is idempotent + the helper is a no-op for
        // stationary-only instruments.
        registerForMobileIfApplicable();
    }

    @Override
    public void stop() {
        transitionTo(State.COOLDOWN);
        unregisterFromMobile();
        perf = null;
    }

    private void registerForMobileIfApplicable() {
        if (perf == null || perf.isPlayer()) return;
        ItemStack stack = perf.instrumentStack();
        if (stack.isEmpty()) return;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return;
        if (!com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping.hasMapping(itemId)) return;
        com.crims.effectiveinstruments.compat.immersivemelodies
                .ImmersiveMelodiesAuraHandler.registerActiveMobileNpc(host.getUUID());
    }

    private void unregisterFromMobile() {
        // Idempotent: safe to call regardless of whether registration happened.
        com.crims.effectiveinstruments.compat.immersivemelodies
                .ImmersiveMelodiesAuraHandler.unregisterActiveMobileNpc(host.getUUID());
    }

    @Override
    public void tick() {
        if (!(host.level() instanceof ServerLevel level)) return;
        stateTicks++;
        switch (state) {
            case READY -> handleReady(level);
            case PLAYING_SUPPORT, PLAYING_OFFENSIVE -> handlePlaying(level);
            case INTERRUPTED -> { if (stateTicks > 30) transitionTo(State.COOLDOWN); }
            case COOLDOWN -> { if (stateTicks > cooldownTicks) transitionTo(State.INACTIVE); }
            default -> {}
        }
    }

    private void handleReady(ServerLevel level) {
        AuraPreset preset = currentPreset();
        if (preset == null) {
            transitionTo(State.INACTIVE);
            return;
        }
        transitionTo(preset.polarity() == Polarity.NEGATIVE
                ? State.PLAYING_OFFENSIVE
                : State.PLAYING_SUPPORT);
    }

    private void handlePlaying(ServerLevel level) {
        perf.emitCue(level);
        StationaryInstrumentNoteService.processNote(perf, perf.instrumentStack());
        if (stateTicks % 20 == 0) {
            if (host.getTarget() != null || host.getLastHurtByMob() != null) {
                transitionTo(State.INTERRUPTED);
                return;
            }
            AuraPreset preset = currentPreset();
            if (preset == null || stateTicks >= preset.getEffectiveDuration()) {
                transitionTo(State.COOLDOWN);
            }
        }
    }

    protected void transitionTo(State next) {
        this.state = next;
        this.stateTicks = 0;
    }

    protected boolean coolDownElapsed() {
        return state != State.COOLDOWN || stateTicks > cooldownTicks;
    }

    protected AuraPreset currentPreset() {
        if (perf == null) return null;
        Optional<ResourceLocation> id = perf.selectedAuraId();
        if (id.isPresent()) {
            return AuraRegistry.getById(id.get().getPath()).orElse(null);
        }
        // 1.6.0 hotfix #4: NPCs have no UI to select an aura. Fall back to the
        // held instrument's default aura — same mapping the player path uses
        // on instrument-screen-open. Without this fallback the goal thrashes
        // INACTIVE → READY → INACTIVE on every tick because currentPreset
        // returns null before AuraManager's auto-select catches up.
        if (!perf.isPlayer()) {
            ItemStack stack = perf.instrumentStack();
            if (stack.isEmpty()) return null;
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) return null;
            String defaultId = InstrumentAuraMapping.getDefaultAuraId(itemId);
            if (defaultId == null) return null;
            return AuraRegistry.getById(defaultId).orElse(null);
        }
        return null;
    }
}

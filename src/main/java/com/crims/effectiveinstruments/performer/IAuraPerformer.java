package com.crims.effectiveinstruments.performer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * The 1.6.0 aura-source abstraction. Any {@link LivingEntity} — player or
 * mod NPC — can act as a performer once wrapped by an adapter discovered
 * through {@code PerformerRegistry}. State machines (note records, durability,
 * targeting) key off {@link #entity()}{@code .getUUID()} so a single player
 * map can hold both player and NPC entries.
 *
 * <p>Stateless about the aura state machine itself; that lives in
 * {@code AuraManager} and {@code ImmersiveMelodiesAuraHandler}.
 */
public interface IAuraPerformer {

    /** The underlying entity. Never null. */
    LivingEntity entity();

    /** True if {@link #entity()} is a {@link ServerPlayer}. */
    default boolean isPlayer() { return entity() instanceof ServerPlayer; }

    /**
     * UUID of the controlling player, or empty for autonomous NPCs (mobs with
     * no owner concept — Custom NPCs, Eidolon raven thralls, etc.).
     */
    Optional<UUID> ownerUuid();

    /**
     * Resolve the controlling player if currently online and authoritative.
     * Returns empty across dimension boundaries when the adapter can't safely
     * cross-resolve. Never throws.
     */
    Optional<ServerPlayer> ownerIfOnline(MinecraftServer srv);

    /** Vanilla scoreboard team, if any. */
    @Nullable
    default Team scoreboardTeam() { return entity().getTeam(); }

    /**
     * Mod-specific faction id (Recruits group UUID, Custom NPCs faction id,
     * MCA village id, …). Empty when the performer's mod has no faction
     * concept.
     */
    default Optional<ResourceLocation> factionId() { return Optional.empty(); }

    /** The instrument stack currently being used. May be empty. */
    ItemStack instrumentStack();

    /** Slot the instrument lives in. Used for swing animation + drop logic. */
    default InteractionHand instrumentHand() { return InteractionHand.MAIN_HAND; }

    /** Aura preset id selected for this performer. */
    Optional<ResourceLocation> selectedAuraId();

    /** Tier the performer is operating in. */
    PerformerTier tier();

    /**
     * Per-tick gate. Called by {@code AuraManager} / mobile pulser before
     * applying effects. Default implementation returns {@code true} so
     * adapters can opt out by overriding.
     */
    default boolean canPerformNow(ServerLevel level) { return true; }

    /**
     * Optional cap that lets the adapter veto per-target classification.
     * Used for Recruits faction diplomacy, MCA village affiliation, etc.
     */
    default TargetingHint classifyTarget(LivingEntity candidate) { return TargetingHint.DEFER; }

    /**
     * Audio/visual cue per note. NPCs override with a {@code SoundEvent}
     * fallback; player default delegates to vanilla swing.
     */
    default void emitCue(ServerLevel lvl) {
        entity().swing(instrumentHand());
    }
}

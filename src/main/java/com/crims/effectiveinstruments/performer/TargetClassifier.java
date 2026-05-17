package com.crims.effectiveinstruments.performer;

import com.crims.effectiveinstruments.aura.EntityCategory;
import com.crims.effectiveinstruments.data.ClassificationOverride;
import com.crims.effectiveinstruments.data.EntityClassificationLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.scores.Team;

import java.util.Set;

/**
 * Generalized version of {@link EntityCategory#classify(ServerPlayer, Entity, Set)}.
 *
 * <p>Resolution order (Phase 0 implements only the player short-circuit + the
 * vanilla bucket; Phase 1 lights up the tag/JSON paths; Phases 2+ add adapter
 * hint + owner/team/faction resolution):
 * <ol>
 *   <li>{@code candidate == performer.entity()} → MUSICIAN</li>
 *   <li>Adapter hint via {@link IAuraPerformer#classifyTarget} → mapped category</li>
 *   <li>Tag override (Phase 1)</li>
 *   <li>JSON override (Phase 1)</li>
 *   <li>Owner-UUID match (Phase 0 implements via {@link OwnerResolver})</li>
 *   <li>Scoreboard team match</li>
 *   <li>Faction (Phases 2+)</li>
 *   <li>Vanilla bucket (delegates to {@link EntityCategory#classify(ServerPlayer, Entity, Set)}
 *       when the performer is a player, else a generalized version of the same logic)</li>
 * </ol>
 *
 * <p>Phase 0 contract: when {@code performer} is a {@link PlayerPerformer},
 * the output matches the legacy classifier byte-for-byte. This is what the
 * parity test gates on.
 */
public final class TargetClassifier {

    /**
     * Generalized classifier consumed by {@code AuraApplicator}. Always returns
     * a non-null category; defaults to {@link EntityCategory#HOSTILE_MOB} for
     * unknown entities so unexpected modded mobs don't accidentally benefit
     * from a positive aura.
     */
    public static EntityCategory classify(
            Entity candidate,
            IAuraPerformer performer,
            Set<ResourceLocation> extraPetTypes
    ) {
        if (!(candidate instanceof LivingEntity living)) {
            return EntityCategory.HOSTILE_MOB;
        }
        if (candidate == performer.entity()) return EntityCategory.MUSICIAN;

        // Adapter hint short-circuit.
        TargetingHint hint = performer.classifyTarget(living);
        if (hint != TargetingHint.DEFER) {
            return mapHint(hint);
        }

        // 1.6.0 Phase 1: tag short-circuits (highest precedence after adapter hint).
        // Per spec §7 — the always_buff / always_debuff / ignore tags let modpack
        // authors wire NPC compat without writing Java.
        if (living.getType().is(EITags.IGNORE)) {
            // IGNORE is mapped to HOSTILE_MOB so polarity gating drops it on the
            // positive path; offensive auras still hit it. Use ALWAYS_DEBUFF when
            // you want true unconditional exclusion.
            return EntityCategory.HOSTILE_MOB;
        }
        if (living.getType().is(EITags.ALWAYS_BUFF))    return EntityCategory.OWN_PET;
        if (living.getType().is(EITags.ALWAYS_DEBUFF))  return EntityCategory.HOSTILE_MOB;
        if (living.getType().is(EITags.TREAT_AS_VILLAGER))   return EntityCategory.VILLAGER;
        if (living.getType().is(EITags.TREAT_AS_IRON_GOLEM)) return EntityCategory.IRON_GOLEM;

        // 1.6.0 Phase 1: JSON entity_classification.json override. Tags
        // short-circuit this; this short-circuits the per-mod adapter default
        // (which lands in the vanilla bucket fall-through below).
        ClassificationOverride override = EntityClassificationLoader.lookup(living.getType());
        if (override.hasCategory()) {
            if (!override.requireTamed() || isTamed(living)) {
                return override.category();
            }
        }
        // delegateTo is parsed and stored but only consulted by Phase 4 MCA
        // hand-off; for Phase 1 we fall through to the default classifier.

        // Phase 0: when the performer is a player, delegate to the legacy
        // classifier so the parity test is byte-identical. Phase 1's tag/JSON
        // overrides above only fire when one matches; the byte-identical
        // player path is unchanged when no tag or JSON entry is present.
        if (performer.entity() instanceof ServerPlayer sp) {
            return EntityCategory.classify(sp, candidate, extraPetTypes);
        }

        // Generalized path for non-player performers: shareOwner first, then
        // scoreboard team, then vanilla bucket. Phase 1 expands this with
        // tag/JSON precedence; Phases 2+ add faction.
        OwnerMatch om = OwnerResolver.shareOwner(living, performer);
        if (om.isOwnerSelf())      return EntityCategory.MUSICIAN; // performer's pet of musician — performer itself is "musician" semantically
        if (om.isSibling())        return EntityCategory.OWN_PET;
        if (om.isOtherPlayer())    return EntityCategory.OTHER_PLAYER;
        if (om.isOtherPlayerPet()) return EntityCategory.OTHER_PLAYER_PET;

        if (sameTeam(living, performer.entity())) return EntityCategory.OWN_PET;

        FactionRelationship fac = FactionResolver.relationship(living, performer.entity());
        if (fac == FactionRelationship.ALLY)  return EntityCategory.OWN_PET;
        if (fac == FactionRelationship.ENEMY) return EntityCategory.HOSTILE_MOB;

        // Vanilla bucket fallback — same checks as the legacy classifier but
        // without the source-player UUID. We synthesize a category from class hierarchy only.
        return vanillaBucket(living, extraPetTypes);
    }

    /** Best-effort scoreboard-team equality. Null-safe. */
    private static boolean sameTeam(LivingEntity a, LivingEntity b) {
        Team ta = a.getTeam();
        Team tb = b.getTeam();
        return ta != null && tb != null && ta == tb;
    }

    /**
     * True if the entity is tamed under one of the vanilla taming systems.
     * Used by {@code entity_classification.json} entries with
     * {@code requireTamed=true} so a default-passive mob like Alex's Mobs
     * Gorilla classifies as OWN_PET only when actually tamed.
     */
    private static boolean isTamed(LivingEntity e) {
        if (e instanceof TamableAnimal t) return t.isTame();
        if (e instanceof AbstractHorse h) return h.isTamed();
        return false;
    }

    /** Map an adapter hint to a category. */
    private static EntityCategory mapHint(TargetingHint hint) {
        return switch (hint) {
            case FORCE_ALLY    -> EntityCategory.OWN_PET;
            case FORCE_ENEMY   -> EntityCategory.HOSTILE_MOB;
            case FORCE_NEUTRAL -> EntityCategory.PASSIVE_MOB;
            case IGNORE        -> EntityCategory.HOSTILE_MOB; // gathered, but excluded by polarity gating in AuraApplicator
            case DEFER         -> EntityCategory.HOSTILE_MOB; // unreachable — caller checks DEFER first
        };
    }

    private static EntityCategory vanillaBucket(LivingEntity living, Set<ResourceLocation> extraPetTypes) {
        // Mirrors the tail of EntityCategory.classify but with no source-UUID dependency.
        if (living instanceof net.minecraft.world.entity.player.Player) return EntityCategory.OTHER_PLAYER;
        if (living instanceof net.minecraft.world.entity.npc.AbstractVillager) return EntityCategory.VILLAGER;
        if (living instanceof net.minecraft.world.entity.animal.IronGolem) return EntityCategory.IRON_GOLEM;
        if (living instanceof net.minecraft.world.entity.animal.Animal
                || living instanceof net.minecraft.world.entity.animal.WaterAnimal) return EntityCategory.PASSIVE_MOB;
        ResourceLocation typeId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        if (typeId != null && extraPetTypes.contains(typeId)) return EntityCategory.PASSIVE_MOB;
        if (living instanceof net.minecraft.world.entity.monster.Enemy) return EntityCategory.HOSTILE_MOB;
        return EntityCategory.HOSTILE_MOB;
    }

    private TargetClassifier() {}
}

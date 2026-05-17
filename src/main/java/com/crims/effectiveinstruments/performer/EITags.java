package com.crims.effectiveinstruments.performer;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Centralized tag keys consumed by {@link TargetClassifier} (Phase 1) and the
 * {@code PerformerRegistry} performer-veto path. Tag JSON files live under
 * {@code data/effective_instruments/tags/entity_types/}; Phase 1 ships empty
 * defaults so modpack authors can extend without conflict.
 *
 * <p>Per spec §7 — these tags are the primary extensibility surface for
 * modpack authors who want to wire NPC compat without writing Java.
 */
public final class EITags {

    private EITags() {}

    /** Force {@link com.crims.effectiveinstruments.aura.EntityCategory#OWN_PET} regardless of owner/team/faction. */
    public static final TagKey<EntityType<?>> ALWAYS_BUFF =
            entityTag("always_buff");

    /** Force {@link com.crims.effectiveinstruments.aura.EntityCategory#HOSTILE_MOB}. */
    public static final TagKey<EntityType<?>> ALWAYS_DEBUFF =
            entityTag("always_debuff");

    /** Skip target entirely — no aura applied regardless of polarity. */
    public static final TagKey<EntityType<?>> IGNORE =
            entityTag("ignore");

    /** Lift a Tier-3 entity into Tier-1 performer eligibility (assumes mainhand instrument detection). */
    public static final TagKey<EntityType<?>> FORCE_PERFORMER =
            entityTag("force_performer");

    /** Veto performer status even if a Tier-1 adapter matches. */
    public static final TagKey<EntityType<?>> NEVER_PERFORMER =
            entityTag("never_performer");

    /** Use VILLAGER bucket + the positive-targeting {@code includeVillagers} flag. */
    public static final TagKey<EntityType<?>> TREAT_AS_VILLAGER =
            entityTag("treat_as_villager");

    /** Use IRON_GOLEM bucket. */
    public static final TagKey<EntityType<?>> TREAT_AS_IRON_GOLEM =
            entityTag("treat_as_iron_golem");

    /** When this entity is a performer, treat its owner player as the aura source for cooldown sharing. */
    public static final TagKey<EntityType<?>> PLAYER_PROXY_OWNER =
            entityTag("player_proxy_owner");

    private static TagKey<EntityType<?>> entityTag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE,
                new ResourceLocation(EffectiveInstrumentsMod.MODID, path));
    }
}

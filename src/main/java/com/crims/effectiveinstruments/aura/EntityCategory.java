package com.crims.effectiveinstruments.aura;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;
import java.util.UUID;

/**
 * Single categorization of a living entity relative to the musician. Drives the
 * polarity-aware targeting in {@link AuraApplicator} — the positive path hard-
 * includes {@link #MUSICIAN} and {@link #OWN_PET}, the offensive path hard-
 * excludes them, and every other category is toggled by its own server-config
 * knob.
 *
 * <p>Ordering matters: {@link #classify} matches from most specific to most
 * generic ({@code MUSICIAN} before {@code OTHER_PLAYER} before pet checks
 * before villagers before golems before passive before hostile). Entities that
 * match nothing land in {@link #HOSTILE_MOB} as the catch-all, which is the
 * safe default for unknown aggressive creatures.
 */
public enum EntityCategory {
    MUSICIAN,
    OWN_PET,
    OTHER_PLAYER,
    OTHER_PLAYER_PET,
    VILLAGER,
    IRON_GOLEM,
    PASSIVE_MOB,
    HOSTILE_MOB;

    /**
     * Classify a candidate entity relative to {@code source}. Pass the cached
     * extra-pet-type allowlist (config-driven, same set as used by the legacy
     * targeting path) so the classifier stays allocation-free.
     */
    public static EntityCategory classify(
            ServerPlayer source, Entity entity, Set<ResourceLocation> extraPetTypes
    ) {
        if (!(entity instanceof LivingEntity)) {
            // Not targetable as an effect recipient — callers should filter these
            // out before calling classify, but bucket as hostile so a stray call
            // doesn't silently hit self/pets.
            return HOSTILE_MOB;
        }
        if (entity == source) return MUSICIAN;

        UUID sourceId = source.getUUID();
        boolean isTamed = isTamedBy(entity, sourceId);
        if (entity instanceof Player) {
            // Players can never be categorized as pets.
            return OTHER_PLAYER;
        }

        // Extra pet types (configured allowlist) — for mobs whose mod doesn't
        // expose a TamableAnimal-style ownership API we can't tell which player
        // owns them. Bucket as PASSIVE_MOB (admin-domesticated) rather than
        // OWN_PET, so positive auras still cover them when the positive
        // allowlist enables PASSIVE_MOB. The actual extra-pet bucketing
        // happens further down once the standard tame-by-anyone checks have
        // run; the OWN_PET return below catches only TamableAnimal/AbstractHorse.
        if (isTamed) return OWN_PET;

        if (isTamedByOther(entity)) return OTHER_PLAYER_PET;

        if (entity instanceof AbstractVillager) return VILLAGER;
        if (entity instanceof IronGolem) return IRON_GOLEM;

        // Animals + water animals that aren't tamed fall into passive.
        if (entity instanceof Animal || entity instanceof WaterAnimal) return PASSIVE_MOB;

        // Extra pet types — once we've ruled out tame-by-anyone, treat them as
        // passive rather than hostile. Admins who allowlist these usually mean
        // "domesticated mob", not "enemy".
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeId != null && extraPetTypes.contains(typeId)) {
            return PASSIVE_MOB;
        }

        // Everything marked as a hostile mob via Forge's Enemy marker — zombies,
        // creepers, pillagers, Wither, Ender Dragon, etc.
        if (entity instanceof Enemy) return HOSTILE_MOB;

        // Unknown living entity — default to hostile so unexpected modded mobs
        // don't benefit from the musician's positive aura by accident.
        return HOSTILE_MOB;
    }

    private static boolean isTamedBy(Entity entity, UUID ownerId) {
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            return ownerId.equals(tamable.getOwnerUUID());
        }
        if (entity instanceof AbstractHorse horse && horse.isTamed()) {
            return ownerId.equals(horse.getOwnerUUID());
        }
        return false;
    }

    private static boolean isTamedByOther(Entity entity) {
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            return tamable.getOwnerUUID() != null;
        }
        if (entity instanceof AbstractHorse horse && horse.isTamed()) {
            return horse.getOwnerUUID() != null;
        }
        return false;
    }
}

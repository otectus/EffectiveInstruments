package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.particle.AuraNoteParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AuraManager {
    private static final Map<UUID, PlayerAuraState> PLAYER_STATES = new ConcurrentHashMap<>();

    public static class PlayerAuraState {
        @Nullable
        AuraPreset selectedAura;
        long lastNoteGameTime = -1;
        boolean instrumentOpen = false;
        // entity network ID -> set of effects we applied to that entity
        final Map<Integer, Set<MobEffect>> affectedTargets = new HashMap<>();

        boolean isActive(long currentGameTime) {
            if (selectedAura == null || !instrumentOpen) return false;
            long window = EIServerConfig.NOTE_WINDOW_TICKS.get();
            return (currentGameTime - lastNoteGameTime) <= window;
        }
    }

    // --- State mutation ---

    public static void setAuraSelection(ServerPlayer player, AuraPreset preset) {
        getOrCreate(player.getUUID()).selectedAura = preset;
        EffectiveInstrumentsMod.LOGGER.debug("Player {} selected aura: {}", player.getName().getString(), preset.id());
    }

    public static void clearAuraSelection(ServerPlayer player) {
        getOrCreate(player.getUUID()).selectedAura = null;
        EffectiveInstrumentsMod.LOGGER.debug("Player {} deselected aura", player.getName().getString());
    }

    public static void onNotePlayed(ServerPlayer player) {
        PlayerAuraState state = getOrCreate(player.getUUID());
        state.lastNoteGameTime = player.level().getGameTime();
    }

    public static void onInstrumentOpen(Player player) {
        getOrCreate(player.getUUID()).instrumentOpen = true;
    }

    public static void onInstrumentClose(Player player) {
        getOrCreate(player.getUUID()).instrumentOpen = false;
    }

    public static void onPlayerLogout(UUID playerId) {
        PLAYER_STATES.remove(playerId);
    }

    @Nullable
    public static PlayerAuraState getState(UUID playerId) {
        return PLAYER_STATES.get(playerId);
    }

    // --- Tick handler ---

    public static void onServerTick(ServerLevel level) {
        if (!EIServerConfig.ENABLED.get()) return;

        long gameTime = level.getGameTime();
        int interval = EIServerConfig.AURA_TICK_INTERVAL.get();

        if (gameTime % interval != 0) return;

        for (ServerPlayer player : level.players()) {
            PlayerAuraState state = PLAYER_STATES.get(player.getUUID());
            if (state == null || !state.isActive(gameTime)) continue;

            AuraPreset aura = state.selectedAura;
            if (aura == null) continue;

            // Validate the aura still exists and is enabled after a potential reload
            Optional<AuraPreset> current = AuraRegistry.getById(aura.id());
            if (current.isEmpty() || !current.get().enabled()) {
                clearPreviousAuraEffects(player, state);
                state.selectedAura = null;
                EffectiveInstrumentsMod.LOGGER.debug(
                        "Cleared stale aura selection '{}' for player {}",
                        aura.id(), player.getName().getString());
                continue;
            }

            applyAuraEffects(player, aura);
            spawnAuraParticles(player, aura);
        }
    }

    private static void applyAuraEffects(ServerPlayer source, AuraPreset aura) {
        int radius = aura.getEffectiveRadius();
        int duration = aura.getEffectiveDuration();
        List<LivingEntity> targets = gatherTargets(source, radius);
        PlayerAuraState state = getOrCreate(source.getUUID());

        for (LivingEntity target : targets) {
            Set<MobEffect> applied = state.affectedTargets
                    .computeIfAbsent(target.getId(), k -> new HashSet<>());
            for (AuraPreset.EffectEntry entry : aura.effects()) {
                if (applyEffectSafely(target, entry.effect(), entry.amplifier(), duration)) {
                    applied.add(entry.effect());
                }
            }
        }
    }

    private static List<LivingEntity> gatherTargets(ServerPlayer source, int radius) {
        List<LivingEntity> targets = new ArrayList<>();
        AABB box = source.getBoundingBox().inflate(radius);

        // Self
        if (EIServerConfig.ALLOW_SELF_BUFF.get()) {
            targets.add(source);
        }

        // Other players
        if (EIServerConfig.INCLUDE_OTHER_PLAYERS.get()) {
            for (Player other : source.serverLevel().getEntitiesOfClass(Player.class, box)) {
                if (other != source && source.distanceToSqr(other) <= (double) radius * radius) {
                    targets.add(other);
                }
            }
        }

        // Tamed pets
        if (EIServerConfig.INCLUDE_TAMED_PETS.get()) {
            Set<ResourceLocation> extraPetTypes = EIServerConfig.PET_ENTITY_ALLOWLIST.get().stream()
                    .map(ResourceLocation::new)
                    .collect(Collectors.toSet());

            for (Entity entity : source.serverLevel().getEntities(source, box)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (source.distanceToSqr(entity) > (double) radius * radius) continue;

                // TamableAnimal (wolf, cat, parrot, etc.)
                if (entity instanceof TamableAnimal tamable
                        && tamable.isTame()
                        && source.getUUID().equals(tamable.getOwnerUUID())) {
                    targets.add(living);
                    continue;
                }

                // AbstractHorse (horse, donkey, mule, llama)
                if (entity instanceof AbstractHorse horse
                        && horse.isTamed()
                        && source.getUUID().equals(horse.getOwnerUUID())) {
                    targets.add(living);
                    continue;
                }

                // Extra pet types from config
                ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                if (typeId != null && extraPetTypes.contains(typeId)) {
                    targets.add(living);
                }
            }
        }

        return targets;
    }

    /**
     * STRONGEST_WINS: only apply if our amplifier >= existing amplifier.
     * Never remove effects we didn't apply.
     * @return true if the effect was actually applied
     */
    private static boolean applyEffectSafely(
            LivingEntity target, MobEffect effect, int amplifier, int duration
    ) {
        MobEffectInstance existing = target.getEffect(effect);
        if (existing != null && existing.getAmplifier() > amplifier) {
            return false;
        }
        target.addEffect(new MobEffectInstance(
                effect, duration, amplifier,
                true,   // ambient (subtle particles)
                true,   // visible
                true    // show icon
        ));
        return true;
    }

    /**
     * Called when the musician switches to a different aura.
     * Strips old aura effects from all tracked targets.
     */
    public static void onAuraSwitch(ServerPlayer player) {
        PlayerAuraState state = PLAYER_STATES.get(player.getUUID());
        if (state == null) return;
        clearPreviousAuraEffects(player, state);
    }

    private static void clearPreviousAuraEffects(ServerPlayer musician, PlayerAuraState state) {
        if (state.affectedTargets.isEmpty() || state.selectedAura == null) return;

        ServerLevel level = musician.serverLevel();
        AuraPreset oldAura = state.selectedAura;

        // Build a map of effect -> amplifier for the old aura so we only strip ours
        Map<MobEffect, Integer> ourEffects = new HashMap<>();
        for (AuraPreset.EffectEntry entry : oldAura.effects()) {
            ourEffects.put(entry.effect(), entry.amplifier());
        }

        for (Map.Entry<Integer, Set<MobEffect>> entry : state.affectedTargets.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) continue;

            for (MobEffect effect : entry.getValue()) {
                Integer ourAmplifier = ourEffects.get(effect);
                if (ourAmplifier == null) continue;

                MobEffectInstance current = living.getEffect(effect);
                if (current == null) continue;

                // Only remove if the effect looks like ours: ambient + matching amplifier
                if (current.isAmbient() && current.getAmplifier() == ourAmplifier) {
                    living.removeEffect(effect);
                }
            }
        }

        state.affectedTargets.clear();
    }

    private static void spawnAuraParticles(ServerPlayer source, AuraPreset aura) {
        ServerLevel level = source.serverLevel();
        int radius = aura.getEffectiveRadius();
        int color = aura.color();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        AuraNoteParticleOptions options = new AuraNoteParticleOptions(r, g, b);
        int count = Math.min(3 + radius / 4, 12);

        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = radius <= 1
                    ? level.random.nextDouble() * Math.max(radius, 0.5)
                    : 1.0 + level.random.nextDouble() * (radius - 1);
            double px = source.getX() + Math.cos(angle) * dist;
            double py = source.getY() + 0.5 + level.random.nextDouble() * 2.0;
            double pz = source.getZ() + Math.sin(angle) * dist;

            level.sendParticles(options, px, py, pz, 1, 0, 0.02, 0, 0);
        }
    }

    private static PlayerAuraState getOrCreate(UUID playerId) {
        return PLAYER_STATES.computeIfAbsent(playerId, k -> new PlayerAuraState());
    }
}

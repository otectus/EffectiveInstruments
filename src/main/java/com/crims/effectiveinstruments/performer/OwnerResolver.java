package com.crims.effectiveinstruments.performer;

import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-mod owner-UUID oracle. The hot-path lookup goes:
 * <ol>
 *   <li>Vanilla {@link OwnableEntity} / {@link TamableAnimal} / {@link AbstractHorse}
 *       short-circuit (no provider call).</li>
 *   <li>Registered {@link OwnerProvider}s, in registration order (each
 *       adapter's provider matches only its mod's entities).</li>
 *   <li>{@link ReflectiveOwnerProbe} fallback for entities with conventional
 *       NBT/synced-data owner fields (caches per {@code Class<?>}).</li>
 * </ol>
 *
 * <p>Used by {@link TargetClassifier} to bucket candidates into MUSICIAN /
 * OWN_PET / OTHER_PLAYER_PET. The relationship resolver
 * {@link #shareOwner(LivingEntity, IAuraPerformer)} builds on top of
 * {@link #ownerOf(LivingEntity)} so a single owner-lookup powers both axes.
 */
public final class OwnerResolver {

    private static final List<OwnerProvider> PROVIDERS = new ArrayList<>();

    public static synchronized void register(OwnerProvider p) {
        PROVIDERS.add(p);
    }

    public static synchronized void clear() {
        PROVIDERS.clear();
    }

    public static Optional<UUID> ownerOf(LivingEntity e) {
        if (e instanceof OwnableEntity o) {
            UUID id = o.getOwnerUUID();
            if (id != null) return Optional.of(id);
        }
        if (e instanceof TamableAnimal t && t.isTame()) {
            UUID id = t.getOwnerUUID();
            if (id != null) return Optional.of(id);
        }
        if (e instanceof AbstractHorse h && h.isTamed()) {
            UUID id = h.getOwnerUUID();
            if (id != null) return Optional.of(id);
        }
        // Snapshot to avoid CME if a provider registers mid-iteration.
        List<OwnerProvider> snapshot;
        synchronized (OwnerResolver.class) { snapshot = List.copyOf(PROVIDERS); }
        for (OwnerProvider p : snapshot) {
            if (p.appliesTo(e)) {
                Optional<UUID> result = p.ownerOf(e);
                if (result.isPresent()) return result;
            }
        }
        return ReflectiveOwnerProbe.tryRead(e);
    }

    /**
     * Determine how {@code candidate} relates to {@code perf} on the
     * owner-UUID axis only. Returns {@link OwnerMatch#NONE} when no
     * relationship is established by ownership — callers fall through to
     * team/faction/vanilla buckets.
     */
    public static OwnerMatch shareOwner(LivingEntity candidate, IAuraPerformer perf) {
        LivingEntity perfEntity = perf.entity();
        if (candidate == perfEntity) {
            return new OwnerMatch(OwnerMatch.Kind.SELF, null, perf.ownerUuid().orElse(null));
        }
        Optional<UUID> perfOwner = perf.ownerUuid();
        Optional<UUID> candOwner = ownerOf(candidate);

        // Candidate is the performer's owner — performer is candidate's pet.
        if (candidate instanceof Player p && perfOwner.isPresent()
                && perfOwner.get().equals(p.getUUID())) {
            return new OwnerMatch(OwnerMatch.Kind.OWNER_SELF, p.getUUID(), perfOwner.get());
        }

        // Both have owners — same owner means sibling pets.
        if (candOwner.isPresent() && perfOwner.isPresent()
                && candOwner.get().equals(perfOwner.get())) {
            return new OwnerMatch(OwnerMatch.Kind.SIBLING, candOwner.get(), perfOwner.get());
        }

        // Candidate has an owner that isn't the performer's.
        if (candOwner.isPresent()) {
            return new OwnerMatch(OwnerMatch.Kind.OTHER_PLAYER_PET, candOwner.get(),
                    perfOwner.orElse(null));
        }

        // Candidate is a player but not the performer's owner.
        if (candidate instanceof Player p) {
            return new OwnerMatch(OwnerMatch.Kind.OTHER_PLAYER, p.getUUID(),
                    perfOwner.orElse(null));
        }
        return OwnerMatch.NONE;
    }

    private OwnerResolver() {}
}

package com.crims.effectiveinstruments.performer;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-mod faction-relationship oracle. Phase 0 ships with the provider list
 * empty; Phase 2 (Recruits) is the first adapter to register a provider.
 *
 * <p>Multiple providers may apply to the same pair (e.g., a Recruits soldier
 * also on a vanilla scoreboard team). Resolution short-circuits on the first
 * non-{@link FactionRelationship#UNKNOWN UNKNOWN} answer; if every provider
 * returns UNKNOWN the caller falls back to scoreboard teams + vanilla
 * classification.
 */
public final class FactionResolver {

    private static final List<FactionProvider> PROVIDERS = new ArrayList<>();

    public static synchronized void register(FactionProvider p) {
        PROVIDERS.add(p);
    }

    public static synchronized void clear() {
        PROVIDERS.clear();
    }

    public static FactionRelationship relationship(LivingEntity a, LivingEntity b) {
        List<FactionProvider> snapshot;
        synchronized (FactionResolver.class) { snapshot = List.copyOf(PROVIDERS); }
        for (FactionProvider p : snapshot) {
            if (!p.appliesTo(a) && !p.appliesTo(b)) continue;
            FactionRelationship rel = p.relationship(a, b);
            if (rel != FactionRelationship.UNKNOWN) return rel;
        }
        return FactionRelationship.UNKNOWN;
    }

    private FactionResolver() {}
}

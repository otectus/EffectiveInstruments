package com.crims.effectiveinstruments.aura;

import com.crims.effectiveinstruments.config.EIServerConfig;

import java.util.EnumSet;

/**
 * Factories for {@link TargetingProfile} instances built from the server
 * config. One per tier × polarity combination. Kept together so the
 * category-to-config mapping is a single screenful of code rather than
 * scattered across callers.
 */
public final class TargetingProfiles {

    /** Stationary-tier positive aura profile. */
    public static TargetingProfile positive() {
        EnumSet<EntityCategory> allowed = EnumSet.noneOf(EntityCategory.class);
        if (EIServerConfig.POSITIVE_INCLUDE_OTHER_PLAYERS.get()) allowed.add(EntityCategory.OTHER_PLAYER);
        if (EIServerConfig.POSITIVE_INCLUDE_OTHER_PLAYER_PETS.get()) allowed.add(EntityCategory.OTHER_PLAYER_PET);
        if (EIServerConfig.POSITIVE_INCLUDE_VILLAGERS.get()) allowed.add(EntityCategory.VILLAGER);
        if (EIServerConfig.POSITIVE_INCLUDE_IRON_GOLEMS.get()) allowed.add(EntityCategory.IRON_GOLEM);
        if (EIServerConfig.POSITIVE_INCLUDE_PASSIVE_MOBS.get()) allowed.add(EntityCategory.PASSIVE_MOB);
        if (EIServerConfig.POSITIVE_INCLUDE_HOSTILE_MOBS.get()) allowed.add(EntityCategory.HOSTILE_MOB);
        return new TargetingProfile(
                allowed,
                EIServerConfig.POSITIVE_MAX_TARGETS_PER_TICK.get(),
                false
        );
    }

    /** Stationary-tier offensive aura profile. */
    public static TargetingProfile offensive() {
        EnumSet<EntityCategory> allowed = EnumSet.noneOf(EntityCategory.class);
        if (EIServerConfig.OFFENSIVE_INCLUDE_ALL_NON_PETS.get()) {
            // 1.4.8 default: hit everything except pets. The musician and their
            // own pets are already excluded for offensive by AuraApplicator's
            // polarity-enforced pet/self check; other-player-pets get excluded
            // by category omission below.
            //
            // 1.4.9 (RECS §3.8): OTHER_PLAYER is gated by the dedicated PvP
            // toggle even with includeAllNonPets=true. This makes PvP-safe
            // servers safe by default when admins flip
            // OFFENSIVE_INCLUDE_OTHER_PLAYERS=false — without this gate the
            // dedicated toggle was silently ignored under the default config.
            if (EIServerConfig.OFFENSIVE_INCLUDE_OTHER_PLAYERS.get()) allowed.add(EntityCategory.OTHER_PLAYER);
            allowed.add(EntityCategory.VILLAGER);
            allowed.add(EntityCategory.IRON_GOLEM);
            allowed.add(EntityCategory.PASSIVE_MOB);
            allowed.add(EntityCategory.HOSTILE_MOB);
        } else {
            if (EIServerConfig.OFFENSIVE_INCLUDE_OTHER_PLAYERS.get()) allowed.add(EntityCategory.OTHER_PLAYER);
            if (EIServerConfig.OFFENSIVE_INCLUDE_OTHER_PLAYER_PETS.get()) allowed.add(EntityCategory.OTHER_PLAYER_PET);
            if (EIServerConfig.OFFENSIVE_INCLUDE_VILLAGERS.get()) allowed.add(EntityCategory.VILLAGER);
            if (EIServerConfig.OFFENSIVE_INCLUDE_IRON_GOLEMS.get()) allowed.add(EntityCategory.IRON_GOLEM);
            if (EIServerConfig.OFFENSIVE_INCLUDE_PASSIVE_MOBS.get()) allowed.add(EntityCategory.PASSIVE_MOB);
            if (EIServerConfig.OFFENSIVE_INCLUDE_HOSTILE_MOBS.get()) allowed.add(EntityCategory.HOSTILE_MOB);
        }
        return new TargetingProfile(
                allowed,
                EIServerConfig.OFFENSIVE_MAX_TARGETS_PER_TICK.get(),
                true
        );
    }

    /**
     * Mobile-tier positive aura profile. Inherits the positive-category toggles
     * (one source of truth for "who is a valid ally target") but uses the
     * mobile-tier's own cap so passive auras stay narrower.
     */
    public static TargetingProfile mobilePositive() {
        TargetingProfile base = positive();
        return new TargetingProfile(
                base.allowedCategories(),
                Math.min(base.maxTargetsPerTick(), EIServerConfig.MOBILE_MAX_TARGETS_PER_TICK.get()),
                false
        );
    }

    /** Mobile-tier offensive aura profile. Same pattern as {@link #mobilePositive}. */
    public static TargetingProfile mobileOffensive() {
        TargetingProfile base = offensive();
        return new TargetingProfile(
                base.allowedCategories(),
                Math.min(base.maxTargetsPerTick(), EIServerConfig.MOBILE_MAX_TARGETS_PER_TICK.get()),
                true
        );
    }

    private TargetingProfiles() {}
}

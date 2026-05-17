package com.crims.effectiveinstruments.performer;

/**
 * Optional hint returned by {@code IAuraPerformer.classifyTarget(LivingEntity)}
 * to short-circuit the default classifier. Adapters use this to express mod-
 * specific friend/foe relationships that can't be derived from owner UUIDs or
 * scoreboard teams alone — e.g., Recruits faction diplomacy, MCA village
 * affiliation, Iron's Spells summon ownership across reflection boundaries.
 *
 * <p>{@link #DEFER} is the default: let the chain (tag → JSON → owner → team
 * → faction → vanilla) decide.
 */
public enum TargetingHint {
    DEFER,
    FORCE_ALLY,
    FORCE_ENEMY,
    FORCE_NEUTRAL,
    IGNORE
}

package com.crims.effectiveinstruments.data;

import com.crims.effectiveinstruments.aura.EntityCategory;

import javax.annotation.Nullable;

/**
 * One row from {@code config/effective_instruments/entity_classification.json}.
 *
 * <ul>
 *   <li>{@code category} — forces the target bucket. Overrides everything except
 *       data tags ({@code effective_instruments:always_buff} / {@code _debuff} /
 *       {@code ignore}) which short-circuit before this layer.</li>
 *   <li>{@code requireTamed} — if true, the override applies only when the
 *       candidate {@code instanceof TamableAnimal} is tamed. Used for entities
 *       like Alex's Mobs Gorilla where untamed = passive but tamed = pet.</li>
 *   <li>{@code delegateTo} — mod id whose adapter should classify this entity
 *       instead of taking the JSON value directly. Used for cross-mod hand-off
 *       (e.g., {@code "mca:villager"} delegates to the MCA adapter for
 *       relationship-aware classification rather than the default villager bucket).</li>
 * </ul>
 */
public record ClassificationOverride(
        @Nullable EntityCategory category,
        boolean requireTamed,
        @Nullable String delegateTo
) {
    public static final ClassificationOverride EMPTY = new ClassificationOverride(null, false, null);

    /** True if this override prescribes a concrete category (no delegation). */
    public boolean hasCategory() {
        return category != null;
    }
}

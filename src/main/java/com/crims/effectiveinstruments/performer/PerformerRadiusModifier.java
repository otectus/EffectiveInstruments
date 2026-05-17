package com.crims.effectiveinstruments.performer;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-mod hook that multiplies the effective aura radius for a given
 * performer. The classic example is {@code compat/pehkui}: when a tamed mob
 * is scaled up via Pehkui, its aura range scales with it.
 *
 * <p>Modifiers compose multiplicatively. The full radius computation in
 * {@link com.crims.effectiveinstruments.aura.AuraApplicator} is:
 * <pre>
 *   effectiveRadius = preset.radius
 *                   * (performer.isPlayer() ? 1.0 : npcs.performerAuraRadiusMultiplier)
 *                   * Product_of(PerformerRadiusModifier.modify(performer, 1.0))
 * </pre>
 *
 * <p>Player path is intentionally never touched by registered modifiers
 * (preserves 1.5.0 parity) — modifiers only run for non-player performers.
 */
public interface PerformerRadiusModifier {

    /**
     * Return {@code baseMultiplier} multiplied by this modifier's contribution.
     * Implementations should return {@code baseMultiplier} unchanged when the
     * performer is outside their concern (cheap pre-check).
     */
    double modify(IAuraPerformer performer, double baseMultiplier);

    /** Static registry used by {@link com.crims.effectiveinstruments.aura.AuraApplicator}. */
    final class Registry {
        private static final List<PerformerRadiusModifier> MODIFIERS = new ArrayList<>();

        public static synchronized void register(PerformerRadiusModifier mod) {
            MODIFIERS.add(mod);
        }
        public static synchronized void clear() { MODIFIERS.clear(); }

        public static double applyAll(IAuraPerformer performer) {
            List<PerformerRadiusModifier> snapshot;
            synchronized (Registry.class) { snapshot = List.copyOf(MODIFIERS); }
            double m = 1.0;
            for (PerformerRadiusModifier mod : snapshot) {
                m = mod.modify(performer, m);
            }
            return m;
        }

        private Registry() {}
    }
}

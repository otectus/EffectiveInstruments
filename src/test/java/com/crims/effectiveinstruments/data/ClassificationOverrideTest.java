package com.crims.effectiveinstruments.data;

import com.crims.effectiveinstruments.aura.EntityCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 unit tests for the {@link ClassificationOverride} record contract.
 *
 * <p>Verifies:
 * <ul>
 *   <li>{@link ClassificationOverride#EMPTY} sentinel is a stable, distinct instance.</li>
 *   <li>{@link ClassificationOverride#hasCategory()} reflects whether {@code category}
 *       was set — the {@link com.crims.effectiveinstruments.performer.TargetClassifier}
 *       fast-path branches on this predicate.</li>
 *   <li>{@code delegateTo} is preserved through the record even when {@code category}
 *       is null (Phase 4 MCA hand-off relies on this shape).</li>
 * </ul>
 */
class ClassificationOverrideTest {

    @Test
    void emptySentinelIsSingleton() {
        assertSame(ClassificationOverride.EMPTY, ClassificationOverride.EMPTY);
        assertFalse(ClassificationOverride.EMPTY.hasCategory());
        assertFalse(ClassificationOverride.EMPTY.requireTamed());
        assertNull(ClassificationOverride.EMPTY.delegateTo());
    }

    @Test
    void hasCategoryReflectsNullCheck() {
        ClassificationOverride withCat =
                new ClassificationOverride(EntityCategory.OWN_PET, false, null);
        assertTrue(withCat.hasCategory());

        ClassificationOverride delegateOnly =
                new ClassificationOverride(null, false, "mca");
        assertFalse(delegateOnly.hasCategory());
    }

    @Test
    void delegateOnlyOverridePreservesShape() {
        // The "delegateTo": "mca" row in entity_classification.json must
        // round-trip through the record with category=null and the delegate id intact.
        ClassificationOverride o = new ClassificationOverride(null, false, "mca");
        assertEquals("mca", o.delegateTo());
        assertNull(o.category());
        assertFalse(o.requireTamed());
    }

    @Test
    void requireTamedFlagIsPreserved() {
        // The Alex's Mobs Gorilla example from the spec: requireTamed=true so
        // wild gorillas remain PASSIVE_MOB but tamed ones become OWN_PET.
        ClassificationOverride o = new ClassificationOverride(EntityCategory.OWN_PET, true, null);
        assertTrue(o.hasCategory());
        assertTrue(o.requireTamed());
        assertEquals(EntityCategory.OWN_PET, o.category());
    }

    @Test
    void allFieldsCanBeSetTogether() {
        // No invariant forbids combining delegateTo with a category — the
        // classifier prefers category when both are present, and Phase 4
        // adapters that respect delegateTo can re-route from there.
        ClassificationOverride o = new ClassificationOverride(EntityCategory.VILLAGER, false, "mca");
        assertTrue(o.hasCategory());
        assertEquals(EntityCategory.VILLAGER, o.category());
        assertEquals("mca", o.delegateTo());
    }
}

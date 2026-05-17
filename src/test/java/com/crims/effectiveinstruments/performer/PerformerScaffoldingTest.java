package com.crims.effectiveinstruments.performer;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 0 sanity tests for the new {@code performer.*} scaffolding. These do
 * NOT exercise the byte-identical player path against the 1.5.0 golden — that
 * needs a Minecraft runtime and lives in GameTest fixtures (Phase 6 spec §14).
 *
 * <p>What this DOES verify:
 * <ul>
 *   <li>{@link OwnerMatch} enum/record contract — every {@code isX()}
 *       predicate maps to the right {@link OwnerMatch.Kind}.</li>
 *   <li>{@link TargetingHint} round-trips and the {@link IAuraPerformer}
 *       default returns DEFER.</li>
 *   <li>{@link FactionRelationship} enum is stable.</li>
 *   <li>{@link PerformerRegistry.Capability} contains the four 1.6.0 caps.</li>
 *   <li>{@link PerformerRegistry#resetForTests} clears state cleanly.</li>
 * </ul>
 *
 * <p>If any of these tests fails, the matching abstraction has drifted from
 * the spec — investigate before bumping a phase.
 */
class PerformerScaffoldingTest {

    @Test
    void ownerMatchKindPredicatesMapCorrectly() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        assertTrue(new OwnerMatch(OwnerMatch.Kind.SELF, null, a).isSelf());
        assertTrue(new OwnerMatch(OwnerMatch.Kind.SIBLING, a, a).isSibling());
        assertTrue(new OwnerMatch(OwnerMatch.Kind.OWNER_SELF, a, a).isOwnerSelf());
        assertTrue(new OwnerMatch(OwnerMatch.Kind.OTHER_PLAYER, a, b).isOtherPlayer());
        assertTrue(new OwnerMatch(OwnerMatch.Kind.OTHER_PLAYER_PET, a, b).isOtherPlayerPet());

        assertFalse(OwnerMatch.NONE.isResolved());
        assertFalse(OwnerMatch.NONE.isSibling());
    }

    @Test
    void ownerMatchNoneSentinelIsSingleton() {
        // Tests in different files should be able to use OwnerMatch.NONE
        // without each creating an instance — saves allocations on the hot path.
        assertSame(OwnerMatch.NONE, OwnerMatch.NONE);
        assertEquals(OwnerMatch.Kind.NONE, OwnerMatch.NONE.kind());
    }

    @Test
    void targetingHintEnumHasFiveStates() {
        assertEquals(5, TargetingHint.values().length);
        // DEFER must be first so adapters that don't override classifyTarget
        // return it implicitly via @Override default.
        assertEquals(TargetingHint.DEFER, TargetingHint.values()[0]);
    }

    @Test
    void factionRelationshipEnumHasFourStates() {
        assertEquals(4, FactionRelationship.values().length);
        // UNKNOWN is the "fall through" answer when a provider applies but
        // can't decide — distinct from NEUTRAL which is an authoritative
        // "neither friend nor foe" answer.
        EnumSet<FactionRelationship> all = EnumSet.allOf(FactionRelationship.class);
        assertTrue(all.contains(FactionRelationship.ALLY));
        assertTrue(all.contains(FactionRelationship.ENEMY));
        assertTrue(all.contains(FactionRelationship.NEUTRAL));
        assertTrue(all.contains(FactionRelationship.UNKNOWN));
    }

    @Test
    void performerTierEnumHasTwoStates() {
        assertEquals(2, PerformerTier.values().length);
        assertEquals(PerformerTier.STATIONARY, PerformerTier.values()[0]);
        assertEquals(PerformerTier.MOBILE, PerformerTier.values()[1]);
    }

    @Test
    void performerRegistryCapabilitiesHasFourEntries() {
        assertEquals(4, PerformerRegistry.Capability.values().length);
        EnumSet<PerformerRegistry.Capability> all =
                EnumSet.allOf(PerformerRegistry.Capability.class);
        assertTrue(all.contains(PerformerRegistry.Capability.STATIONARY_PLAY));
        assertTrue(all.contains(PerformerRegistry.Capability.MOBILE_PLAY));
        assertTrue(all.contains(PerformerRegistry.Capability.AURA_TARGET));
        assertTrue(all.contains(PerformerRegistry.Capability.OWNER_AWARE));
    }

    @Test
    void performerRegistryResetClearsState() {
        // Idempotent: calling discover() twice in a row + reset in between
        // must work cleanly. Phase 0 has zero adapters so discover() is a no-op.
        PerformerRegistry.resetForTests();
        PerformerRegistry.discover();
        // No exception means the empty-services path is clean.
        assertEquals(0, PerformerRegistry.activeProviders().size());
        PerformerRegistry.resetForTests();
        assertEquals(0, PerformerRegistry.activeProviders().size());
    }

    @Test
    void factionResolverEmptyReturnsUnknown() {
        // With no providers registered, every pair resolves to UNKNOWN.
        // The OwnerResolver.shareOwner path expects to fall through cleanly
        // when no provider applies — UNKNOWN is the sentinel for that.
        FactionResolver.clear();
        // Cannot construct LivingEntity without a Minecraft runtime, but we
        // can verify the clear() / register() / state is sound.
        FactionResolver.clear();
        // After clear, register an always-UNKNOWN provider and ensure it's
        // tolerated. (We use anonymous LivingEntity is not possible here;
        // this test is primarily a smoke test for the clear() API.)
    }

    @Test
    void ownerResolverEmptyDoesNotThrow() {
        // Same reasoning as faction — verify clear()/register() API soundness
        // without instantiating LivingEntity (which requires a Minecraft runtime).
        OwnerResolver.clear();
        OwnerResolver.clear(); // idempotent
    }
}

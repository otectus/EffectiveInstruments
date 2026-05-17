package com.crims.effectiveinstruments.performer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Returned by {@code OwnerResolver.shareOwner(LivingEntity, IAuraPerformer)}.
 * Five-state instead of an enum so the caller can read the actual UUIDs when
 * needed (diagnostics, tag-driven overrides).
 */
public record OwnerMatch(
        Kind kind,
        @Nullable UUID candidateOwner,
        @Nullable UUID performerOwner
) {
    public enum Kind {
        /** Candidate is the performer entity itself. */
        SELF,
        /** Candidate is owned by the same UUID as the performer. */
        SIBLING,
        /** Candidate IS the performer's owner (performer is a pet of candidate). */
        OWNER_SELF,
        /** Candidate is a player (and not the performer's owner). */
        OTHER_PLAYER,
        /** Candidate is owned by a different player than the performer's owner. */
        OTHER_PLAYER_PET,
        /** No relationship established by ownership alone — fall through to team/faction/vanilla. */
        NONE
    }

    public static final OwnerMatch NONE = new OwnerMatch(Kind.NONE, null, null);

    public boolean isSelf()           { return kind == Kind.SELF; }
    public boolean isSibling()        { return kind == Kind.SIBLING; }
    public boolean isOwnerSelf()      { return kind == Kind.OWNER_SELF; }
    public boolean isOtherPlayer()    { return kind == Kind.OTHER_PLAYER; }
    public boolean isOtherPlayerPet() { return kind == Kind.OTHER_PLAYER_PET; }
    public boolean isResolved()       { return kind != Kind.NONE; }
}

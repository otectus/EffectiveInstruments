package com.crims.effectiveinstruments.performer;

/**
 * Return value of {@code FactionProvider.relationship(a, b)}. UNKNOWN means
 * the provider applies to one or both entities but couldn't determine a
 * concrete relationship — callers should treat it the same as NEUTRAL but
 * are free to fall through to a different provider.
 */
public enum FactionRelationship {
    ALLY,
    ENEMY,
    NEUTRAL,
    UNKNOWN
}

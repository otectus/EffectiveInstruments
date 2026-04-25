package com.crims.effectiveinstruments.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Shared atomic-write helper for every config writer in the mod. Promoted
 * from {@code InstrumentAuraMapping.writeAtomically} in 1.4.9 so the rest
 * of the writers (durability, mobile mapping, preset JSONs, READMEs) share
 * the same crash-safety guarantee — a power-loss mid-write can never
 * truncate the real file.
 *
 * <p>Writes to a sibling {@code .tmp} then moves with
 * {@code ATOMIC_MOVE | REPLACE_EXISTING}. Falls back to a plain replace
 * when the underlying filesystem rejects atomic moves (some Windows + FAT
 * configurations).
 */
public final class ConfigIO {

    public static void writeAtomically(Path target, String content) throws IOException {
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);

        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ConfigIO() {}
}

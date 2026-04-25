package com.crims.effectiveinstruments.compat.immersivemelodies;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Quarantined bridge to Immersive Melodies. <b>Imports no IM classes</b> —
 * the only coupling is the mod id string, the NBT key {@code "playing"} that IM
 * writes to an instrument's ItemStack, and the registry-id match against the
 * mobile mapping. If IM changes its NBT format, this bridge silently falls
 * back to "no mobile buff" rather than crashing.
 *
 * <p>The upgrade doc originally proposed a {@code Class.forName} + reflection
 * bridge; we dropped it in favor of NBT+registry matching because:
 * <ul>
 *   <li>No {@code NoClassDefFoundError} risk when IM is absent.</li>
 *   <li>IM's NBT keys are part of its save format, so they're effectively frozen.</li>
 *   <li>The hot path (per-player per-pulse) avoids reflection.</li>
 * </ul>
 *
 * <p>Free-play coverage: this bridge only sees {@code playing=true} (autoplay
 * + selected-melody playback). Free-play keyboard / MIDI input doesn't flip
 * the NBT, so the screen-open path on
 * {@link ImmersiveMelodiesAuraHandler#onScreenOpened} (1.4.3+) covers that
 * case via the client-side {@code InstrumentOpenC2SPacket}.
 */
public final class ImmersiveMelodiesCompat {

    /** Mod id as declared in IM's {@code mods.toml}. */
    public static final String MODID = "immersive_melodies";

    /** NBT key IM writes on the held stack while a melody is being played. */
    private static final String PLAYING_TAG_KEY = "playing";

    private static boolean available = false;

    public static void init() {
        available = ModList.get().isLoaded(MODID);
        if (available) {
            EffectiveInstrumentsMod.LOGGER.info(
                    "Immersive Melodies detected — mobile tier compat enabled");
        } else {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "Immersive Melodies not present — mobile tier compat inactive");
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * Find the first held stack (main or off hand) whose registry id has a mobile
     * mapping entry AND whose NBT has {@code playing=true}. Returns {@code null}
     * if no hand slot qualifies. Also returns null when compat is unavailable.
     *
     * <p>The mapping lookup is cheap (a HashMap contains check) and prevents us
     * from reading NBT on unrelated items like swords.
     */
    @Nullable
    public static HeldInstrument findActivePlayingStack(ServerPlayer player) {
        if (!available) return null;

        for (ItemStack stack : player.getHandSlots()) {
            if (stack.isEmpty()) continue;
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) continue;
            if (!MobileInstrumentAuraMapping.hasMapping(itemId)) continue;
            if (!isMarkedPlaying(stack)) continue;
            return new HeldInstrument(itemId, stack);
        }
        return null;
    }

    /**
     * Null-safe read of the {@code playing} flag on an instrument stack. Returns
     * false if the stack has no tag, the key is missing, or the key is any
     * non-boolean shape — all of which are "not playing" semantically.
     */
    private static boolean isMarkedPlaying(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        if (!tag.contains(PLAYING_TAG_KEY)) return false;
        return tag.getBoolean(PLAYING_TAG_KEY);
    }

    /** Result of {@link #findActivePlayingStack}: the instrument's registry id and its backing stack. */
    public record HeldInstrument(ResourceLocation instrumentId, ItemStack stack) {}

    private ImmersiveMelodiesCompat() {}
}

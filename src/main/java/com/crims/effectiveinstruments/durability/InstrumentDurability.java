package com.crims.effectiveinstruments.durability;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.config.InstrumentDurabilityConfig;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * NBT-backed durability for foreign instrument items. The mod does not own any
 * instrument items (they come from Genshin Instruments, Even More Instruments,
 * and Immersive Melodies), so we can't set {@code maxDamage} on the
 * {@link net.minecraft.world.item.Item} class. Instead we track a single int
 * under the tag key {@value #TAG_KEY} on the stack, initialised lazily on first
 * access.
 *
 * <p>When the durability value is missing from the stack's NBT, the helper
 * treats the instrument as full (max). That means existing instruments in
 * 1.3.x worlds "just work" when a 1.4.0 install is loaded — the first hover or
 * first play call triggers initialisation.
 */
public final class InstrumentDurability {
    /**
     * Stack-NBT key for the current durability value. Prefixed to avoid collisions
     * with vanilla and other mods. Value type: int.
     */
    public static final String TAG_KEY = "EIDurability";

    /**
     * Per-Item cached tracking result. Registry lookups are hot — `getCurrent`
     * and `getMax` are called on every item-slot render frame for every tracked
     * stack, plus the durability-bar decorator calls `isTracked` on every item
     * render pass. Caching avoids a `ForgeRegistries.ITEMS.getKey()` per frame.
     * Invalidated via {@link #invalidateEntryCache} when the durability config
     * reloads.
     */
    private static final java.util.Map<net.minecraft.world.item.Item, InstrumentDurabilityConfig.Entry> ENTRY_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();
    /** Sentinel for "item known to have no entry"; placing it in the cache avoids repeat misses. */
    private static final InstrumentDurabilityConfig.Entry NO_ENTRY =
            new InstrumentDurabilityConfig.Entry(-1, null, 0);

    @Nullable
    private static InstrumentDurabilityConfig.Entry resolveEntry(ItemStack stack) {
        if (stack.isEmpty()) return null;
        InstrumentDurabilityConfig.Entry cached = ENTRY_CACHE.get(stack.getItem());
        if (cached != null) return cached == NO_ENTRY ? null : cached;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        InstrumentDurabilityConfig.Entry entry = id == null
                ? null
                : InstrumentDurabilityConfig.get(id);
        // 1.4.9 (RECS §1.1): when an item from a known instrument namespace
        // isn't pre-listed in instrument_durability.json, synthesize a default
        // Entry from EIServerConfig.DURABILITY_DEFAULT_MAX. Without this,
        // third-party instruments and any newly-added shipped instrument the
        // config doesn't yet cover get zero durability tracking — exactly the
        // opposite of what InstrumentDurabilityConfig's class doc promises.
        // The namespace gate keeps non-instrument items (sword, shield, etc.)
        // from getting accidental durability tracking.
        if (entry == null && InstrumentNamespaces.contains(id)) {
            entry = synthesizeDefaultEntry();
        }
        ENTRY_CACHE.put(stack.getItem(), entry == null ? NO_ENTRY : entry);
        return entry;
    }

    /**
     * Build a fallback {@link InstrumentDurabilityConfig.Entry} from the
     * server-config defaults. Returns {@code null} when SERVER config isn't
     * loaded yet (pre-join client, or common-setup before
     * {@code ServerAboutToStartEvent}) so the cache slot is filled with
     * {@link #NO_ENTRY} and re-resolved after {@link #invalidateEntryCache}
     * runs from the server-start handler.
     */
    @Nullable
    private static InstrumentDurabilityConfig.Entry synthesizeDefaultEntry() {
        try {
            int defaultMax = EIServerConfig.DURABILITY_DEFAULT_MAX.get();
            return new InstrumentDurabilityConfig.Entry(
                    Math.max(1, defaultMax),
                    null,
                    Math.max(1, defaultMax / 5));
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    /** Drop cached entries; call after {@link InstrumentDurabilityConfig#load}. */
    public static void invalidateEntryCache() {
        ENTRY_CACHE.clear();
    }

    /**
     * Current durability of a stack. Returns the configured max if the tag is
     * absent and the stack is a known instrument, or {@code 0} if the stack is
     * not a tracked instrument at all.
     */
    public static int getCurrent(ItemStack stack) {
        InstrumentDurabilityConfig.Entry entry = resolveEntry(stack);
        if (entry == null) return 0;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_KEY)) {
            return entry.maxDurability();
        }
        int raw = tag.getInt(TAG_KEY);
        return Math.max(0, Math.min(raw, entry.maxDurability()));
    }

    /**
     * Configured max durability for the stack's item, or the global
     * {@code DURABILITY_DEFAULT_MAX} when the item isn't in the mapping. Returns
     * {@code 0} for non-item stacks.
     */
    public static int getMax(ItemStack stack) {
        InstrumentDurabilityConfig.Entry entry = resolveEntry(stack);
        return entry == null ? 0 : entry.maxDurability();
    }

    /** True only if the stack is a tracked instrument (has a max durability). */
    public static boolean isTracked(ItemStack stack) {
        return getMax(stack) > 0;
    }

    /** True when durability is at 0 and further play should be gated. */
    public static boolean isBroken(ItemStack stack) {
        if (!EIServerConfig.DURABILITY_ENABLED.get()) return false;
        if (!isTracked(stack)) return false;
        return getCurrent(stack) <= 0;
    }

    /**
     * Subtract {@code amount} from the stack's durability, clamping to 0. If the
     * value transitions from {@code >0} to {@code ≤0}, plays the vanilla
     * item-break sound and spawns break particles. Caller is responsible for
     * routing the "aura turns off" signal (see
     * {@link com.crims.effectiveinstruments.aura.AuraManager#onInstrumentClose}
     * and the IM handler's {@code onExplicitClear}).
     *
     * @param player nullable — used to position the break sound/particles. When
     *               null, the transition still fires but without visual feedback.
     * @return {@code true} when this call caused the transition to broken.
     */
    public static boolean damage(ItemStack stack, int amount, @Nullable ServerPlayer player) {
        if (!EIServerConfig.DURABILITY_ENABLED.get()) return false;
        if (amount <= 0) return false;
        if (!isTracked(stack)) return false;
        // Creative immunity: matches vanilla tool behavior by default. Admins
        // and testers can flip `durability.creativeImmunity = false` in
        // server.toml to verify depletion in creative mode.
        if (player != null
                && player.getAbilities().instabuild
                && EIServerConfig.DURABILITY_CREATIVE_IMMUNITY.get()) {
            return false;
        }

        int before = getCurrent(stack);
        if (before <= 0) return false;

        int after = Math.max(0, before - amount);
        stack.getOrCreateTag().putInt(TAG_KEY, after);

        if (after == 0 && before > 0) {
            onBreakTransition(stack, player);
            return true;
        }
        return false;
    }

    /**
     * Restore {@code amount} points to the stack's durability, clamping to max.
     * Returns the new current value. Does nothing when durability is disabled
     * or the item is untracked.
     */
    public static int repair(ItemStack stack, int amount) {
        if (!EIServerConfig.DURABILITY_ENABLED.get()) return getCurrent(stack);
        if (amount <= 0) return getCurrent(stack);
        if (!isTracked(stack)) return 0;

        int max = getMax(stack);
        int current = getCurrent(stack);
        int next = Math.min(max, current + amount);
        stack.getOrCreateTag().putInt(TAG_KEY, next);
        return next;
    }

    /** Set durability directly (used by the admin subcommand). Clamps to [0, max]. */
    public static int set(ItemStack stack, int value) {
        if (!isTracked(stack)) return 0;
        int clamped = Math.max(0, Math.min(value, getMax(stack)));
        stack.getOrCreateTag().putInt(TAG_KEY, clamped);
        return clamped;
    }

    private static void onBreakTransition(ItemStack stack, @Nullable ServerPlayer player) {
        if (player == null) return;
        ServerLevel level = player.serverLevel();
        try {
            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS,
                    0.8f, 0.9f + level.random.nextFloat() * 0.2f
            );
            level.sendParticles(
                    new ItemParticleOption(ParticleTypes.ITEM, stack),
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    10, 0.2, 0.2, 0.2, 0.05
            );
        } catch (Exception e) {
            EffectiveInstrumentsMod.LOGGER.debug("Failed to spawn break feedback", e);
        }
    }

    private InstrumentDurability() {}
}

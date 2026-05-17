package com.crims.effectiveinstruments.performer;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Discovers and dispatches {@link PerformerAdapterProvider}s through the JVM
 * {@link ServiceLoader} mechanism.
 *
 * <p>Each per-mod adapter ships a single-line resource at
 * {@code META-INF/services/com.crims.effectiveinstruments.performer.PerformerRegistry$PerformerAdapterProvider}
 * listing its provider FQN. {@link #discover()} runs once at common-setup,
 * loads every provider, and keeps the ones whose mod is actually present at
 * runtime.
 *
 * <p>{@link #wrap(LivingEntity, PerformerTier)} short-circuits on
 * {@link ServerPlayer} so the player path costs one {@code instanceof} check
 * — no adapter walk.
 */
public final class PerformerRegistry {

    /**
     * Capabilities an adapter declares so the discovery log line is informative
     * and {@code /effectiveinstruments npcs adapters} can render columns.
     */
    public enum Capability {
        STATIONARY_PLAY,
        MOBILE_PLAY,
        AURA_TARGET,
        OWNER_AWARE
    }

    /**
     * The interface every per-mod adapter implements + registers via
     * {@link ServiceLoader}.
     */
    public interface PerformerAdapterProvider {
        /** Forge mod id this adapter targets — must match the {@code mods.toml} entry. */
        String modId();

        /** True if the target mod is actually loaded in this runtime. */
        boolean isLoaded();

        /**
         * Per-adapter bootstrap. Runs once at common-setup if {@link #isLoaded()}.
         * Adapters use this to register {@code OwnerProvider}, {@code FactionProvider},
         * Forge bus listeners, etc.
         */
        default void bootstrap(IEventBus modBus) {}

        /**
         * Wrap a {@link LivingEntity} as an {@link IAuraPerformer} if this
         * adapter recognizes the entity type, else {@code Optional.empty()}.
         */
        Optional<IAuraPerformer> wrap(LivingEntity e, PerformerTier requested);

        /** Declared capabilities for diagnostics + scheduling. */
        EnumSet<Capability> capabilities();
    }

    private static final List<PerformerAdapterProvider> ACTIVE = new ArrayList<>();
    private static volatile boolean discovered = false;

    /**
     * Idempotent. Runs the {@link ServiceLoader} sweep, calls {@code bootstrap}
     * on every loaded provider, and logs the active set.
     */
    public static synchronized void discover() {
        if (discovered) return;
        discovered = true;
        for (PerformerAdapterProvider provider : ServiceLoader.load(PerformerAdapterProvider.class,
                PerformerRegistry.class.getClassLoader())) {
            try {
                if (provider.isLoaded()) {
                    ACTIVE.add(provider);
                    EffectiveInstrumentsMod.LOGGER.info(
                            "EI performer adapter active: {} capabilities={}",
                            provider.modId(), provider.capabilities());
                } else {
                    EffectiveInstrumentsMod.LOGGER.debug(
                            "EI performer adapter dormant ({} not installed): {}",
                            provider.modId(), provider.getClass().getName());
                }
            } catch (Throwable t) {
                EffectiveInstrumentsMod.LOGGER.error(
                        "EI performer adapter {} failed isLoaded() check",
                        provider.getClass().getName(), t);
            }
        }
        if (ACTIVE.isEmpty()) {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "EI performer adapters: none active (player-only aura source)");
        }
    }

    /**
     * Run the per-adapter {@code bootstrap} step. Decoupled from
     * {@link #discover()} so callers can stagger the two: discovery happens
     * early enough to populate the active list; bootstrap can wait until the
     * SERVER config is loaded if an adapter needs config reads.
     */
    public static synchronized void bootstrapAll(IEventBus modBus) {
        for (PerformerAdapterProvider provider : ACTIVE) {
            try {
                provider.bootstrap(modBus);
            } catch (Throwable t) {
                EffectiveInstrumentsMod.LOGGER.error(
                        "EI performer adapter {} threw during bootstrap",
                        provider.modId(), t);
            }
        }
    }

    /**
     * Wrap an entity as a performer. Player short-circuit means the player
     * path is never gated on adapter iteration. Returns {@code Optional.empty()}
     * for entities no adapter recognizes.
     */
    public static Optional<IAuraPerformer> wrap(LivingEntity e, PerformerTier tier) {
        if (e instanceof ServerPlayer sp) {
            return Optional.of(new PlayerPerformer(sp, tier));
        }
        List<PerformerAdapterProvider> snapshot;
        synchronized (PerformerRegistry.class) { snapshot = List.copyOf(ACTIVE); }
        for (PerformerAdapterProvider provider : snapshot) {
            try {
                Optional<IAuraPerformer> wrapped = provider.wrap(e, tier);
                if (wrapped.isPresent()) return wrapped;
            } catch (Throwable t) {
                EffectiveInstrumentsMod.LOGGER.warn(
                        "EI performer adapter {} threw during wrap of {}",
                        provider.modId(), e.getType(), t);
            }
        }
        return Optional.empty();
    }

    /** Used by {@code /effectiveinstruments npcs adapters}. */
    public static synchronized List<PerformerAdapterProvider> activeProviders() {
        return List.copyOf(ACTIVE);
    }

    /** Test/reload only. */
    public static synchronized void resetForTests() {
        ACTIVE.clear();
        discovered = false;
    }

    private PerformerRegistry() {}
}

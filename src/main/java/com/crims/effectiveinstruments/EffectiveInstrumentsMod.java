package com.crims.effectiveinstruments;

import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.command.EICommands;
import com.crims.effectiveinstruments.compat.genshin.GenshinInstrumentsCompat;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesCompat;
import com.crims.effectiveinstruments.config.EIClientConfig;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import com.crims.effectiveinstruments.network.EIPacketHandler;
import com.crims.effectiveinstruments.particle.EIParticleTypes;
import com.crims.effectiveinstruments.performer.PerformerRegistry;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod(EffectiveInstrumentsMod.MODID)
public class EffectiveInstrumentsMod {
    public static final String MODID = "effectiveinstruments";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EffectiveInstrumentsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        EIParticleTypes.PARTICLE_TYPES.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EIServerConfig.SPEC,
                "effective_instruments/server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, EIClientConfig.SPEC,
                "effective_instruments/client.toml");

        // Register Forge-bus listeners. ServerAboutToStartEvent is the earliest
        // point at which the SERVER config spec is guaranteed to be loaded —
        // any code that reads EIServerConfig.X.get() at common-setup time runs
        // before that, so we defer config-dependent work to onServerAboutToStart.
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);

        // Warn about old config files from before the subfolder migration
        checkOldConfigMigration();

        LOGGER.info("Effective Instruments initializing");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            EIPacketHandler.register();
            // 1.4.9: only the JSON-loading half runs here. SERVER-config reads
            // (durability default fallback, pet allowlist cache invalidation)
            // are deferred to ServerAboutToStartEvent — see #1.2 in
            // RECOMMENDATIONS.md. Forge SERVER configs are not loaded yet.
            AuraRegistry.loadPresetsAndMappings();
            // 1.5.0: Genshin Instruments and Immersive Melodies are now both
            // optional backends. Each compat init is a no-op when its mod is
            // absent; the dual-absence warn below tells admins to install one
            // of them so EI's gameplay features actually do something.
            GenshinInstrumentsCompat.initCommon();
            ImmersiveMelodiesCompat.init();
            if (!GenshinInstrumentsCompat.isAvailable() && !ImmersiveMelodiesCompat.isAvailable()) {
                LOGGER.warn("No supported instrument backend detected. Install Genshin Instruments " +
                        "or Immersive Melodies to activate Effective Instruments gameplay features.");
            }
            // 1.6.0: discover per-mod NPC performer adapters via ServiceLoader.
            // Adapter providers ship as META-INF/services entries; each one
            // registers its OwnerProvider/FactionProvider + Forge bus listeners
            // during bootstrap. Phase 0 ships zero adapters; later phases add 22.
            PerformerRegistry.discover();
            PerformerRegistry.bootstrapAll(FMLJavaModLoadingContext.get().getModEventBus());
        });
    }

    private void onServerAboutToStart(final ServerAboutToStartEvent event) {
        // SERVER-config-dependent tail of registry init. Runs once per world
        // load (and again on /effectiveinstruments reload, which calls both
        // halves). Order matters: refresh the registry's config-derived caches
        // first, then flush InstrumentDurability so the next stack hover picks
        // up the just-loaded DURABILITY_DEFAULT_MAX value.
        AuraRegistry.refreshConfigDerived();
        InstrumentDurability.invalidateEntryCache();
        EIServerConfig.warnDeprecated();
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        EICommands.register(event.getDispatcher());
    }

    private void checkOldConfigMigration() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path oldServer = configDir.resolve("effectiveinstruments-server.toml");
        Path oldClient = configDir.resolve("effectiveinstruments-client.toml");
        if (Files.exists(oldServer) || Files.exists(oldClient)) {
            LOGGER.warn("==========================================================");
            LOGGER.warn("Effective Instruments config files have moved!");
            LOGGER.warn("Old location: config/effectiveinstruments-*.toml");
            LOGGER.warn("New location: config/effective_instruments/*.toml");
            LOGGER.warn("Please migrate your settings and delete the old files.");
            LOGGER.warn("==========================================================");
        }
    }
}

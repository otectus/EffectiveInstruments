package com.crims.effectiveinstruments;

import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.command.EICommands;
import com.crims.effectiveinstruments.config.EIClientConfig;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.network.EIPacketHandler;
import com.crims.effectiveinstruments.particle.EIParticleTypes;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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

        // Register command listener on FORGE bus
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Warn about old config files from before the subfolder migration
        checkOldConfigMigration();

        LOGGER.info("Effective Instruments initializing");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            EIPacketHandler.register();
            AuraRegistry.load();
        });
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

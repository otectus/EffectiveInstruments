package com.crims.effectiveinstruments.client.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.aura.InstrumentAuraMapping;
import com.crims.effectiveinstruments.client.widget.AuraSelectorWidget;
import com.crims.effectiveinstruments.config.EIClientConfig;
import com.crims.effectiveinstruments.network.EIPacketHandler;
import com.crims.effectiveinstruments.network.packet.InstrumentOpenC2SPacket;
import com.crims.effectiveinstruments.network.packet.SelectAuraC2SPacket;
import com.cstav.genshinstrument.client.gui.screen.instrument.partial.InstrumentScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        modid = EffectiveInstrumentsMod.MODID,
        value = Dist.CLIENT
)
public class AuraOverlayInjector {

    @Nullable
    private static AuraSelectorWidget selectorWidget = null;
    @Nullable
    private static String currentSelectedAuraId = null;
    @Nullable
    private static ResourceLocation currentInstrumentId = null;

    // Per-instrument aura overrides remembered within the session
    private static final Map<ResourceLocation, String> instrumentAuraOverrides = new HashMap<>();

    @SubscribeEvent
    public static void onScreenInit(final ScreenEvent.Init.Post event) {
        if (!EIClientConfig.SHOW_OVERLAY.get()) return;
        if (!AuraRegistry.isLoaded()) return;

        if (!isInstrumentScreen(event.getScreen())) return;

        Screen screen = event.getScreen();

        // Extract instrument ID and notify server
        if (screen instanceof InstrumentScreen instrumentScreen) {
            currentInstrumentId = instrumentScreen.getInstrumentId();
            EIPacketHandler.sendToServer(new InstrumentOpenC2SPacket(currentInstrumentId));

            // Check for a remembered per-instrument override
            String override = instrumentAuraOverrides.get(currentInstrumentId);
            if (override != null) {
                currentSelectedAuraId = override;
                // Tell server to use the override instead of the default
                EIPacketHandler.sendToServer(new SelectAuraC2SPacket(override));
            }
        }

        selectorWidget = new AuraSelectorWidget(
                screen,
                InstrumentAuraMapping.getAllowedAuras(currentInstrumentId),
                currentSelectedAuraId,
                AuraOverlayInjector::onAuraSelected
        );

        event.addListener(selectorWidget);
    }

    @SubscribeEvent
    public static void onScreenClose(final ScreenEvent.Closing event) {
        if (selectorWidget != null && event.getScreen() == selectorWidget.parentScreen) {
            // Save override if player has a selection
            if (currentInstrumentId != null && currentSelectedAuraId != null) {
                instrumentAuraOverrides.put(currentInstrumentId, currentSelectedAuraId);
            } else if (currentInstrumentId != null) {
                instrumentAuraOverrides.remove(currentInstrumentId);
            }
            selectorWidget = null;
            currentSelectedAuraId = null;
            currentInstrumentId = null;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(final ClientPlayerNetworkEvent.LoggingOut event) {
        instrumentAuraOverrides.clear();
        currentSelectedAuraId = null;
        currentInstrumentId = null;
        selectorWidget = null;
    }

    private static boolean isInstrumentScreen(Screen screen) {
        // Primary: InstrumentScreen from Genshin Instruments (also catches Even More Instruments)
        if (screen instanceof InstrumentScreen) return true;

        // Fallback: config allowlist for other instrument mods
        String className = screen.getClass().getName();
        return EIClientConfig.SCREEN_CLASS_ALLOWLIST.get().contains(className);
    }

    private static void onAuraSelected(@Nullable AuraPreset preset) {
        if (preset == null) {
            currentSelectedAuraId = null;
            if (currentInstrumentId != null) {
                instrumentAuraOverrides.remove(currentInstrumentId);
            }
            EIPacketHandler.sendToServer(new SelectAuraC2SPacket(""));
        } else {
            currentSelectedAuraId = preset.id();
            if (currentInstrumentId != null) {
                instrumentAuraOverrides.put(currentInstrumentId, preset.id());
            }
            EIPacketHandler.sendToServer(new SelectAuraC2SPacket(preset.id()));
        }
    }

    /**
     * Called by the server sync packet when a default aura is auto-selected.
     * Only applies if the client has no override for the current instrument.
     */
    public static void onServerSyncAura(String auraId) {
        // Client override takes precedence
        if (currentInstrumentId != null && instrumentAuraOverrides.containsKey(currentInstrumentId)) {
            return;
        }
        currentSelectedAuraId = auraId.isEmpty() ? null : auraId;
        if (selectorWidget != null) {
            selectorWidget.setSelectedAuraId(currentSelectedAuraId);
        }
    }
}

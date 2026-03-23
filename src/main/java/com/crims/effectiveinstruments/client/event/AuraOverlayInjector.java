package com.crims.effectiveinstruments.client.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.client.widget.AuraSelectorWidget;
import com.crims.effectiveinstruments.config.EIClientConfig;
import com.crims.effectiveinstruments.network.EIPacketHandler;
import com.crims.effectiveinstruments.network.packet.SelectAuraC2SPacket;
import com.cstav.genshinstrument.client.gui.screen.instrument.partial.InstrumentScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

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

    @SubscribeEvent
    public static void onScreenInit(final ScreenEvent.Init.Post event) {
        if (!EIClientConfig.SHOW_OVERLAY.get()) return;
        if (!AuraRegistry.isLoaded()) return;

        if (!isInstrumentScreen(event.getScreen())) return;

        Screen screen = event.getScreen();
        selectorWidget = new AuraSelectorWidget(
                screen,
                AuraRegistry.getEnabledPresets(),
                currentSelectedAuraId,
                AuraOverlayInjector::onAuraSelected
        );

        event.addListener(selectorWidget);
    }

    @SubscribeEvent
    public static void onScreenClose(final ScreenEvent.Closing event) {
        if (selectorWidget != null && event.getScreen() == selectorWidget.parentScreen) {
            selectorWidget = null;
            // Keep currentSelectedAuraId so selection persists across screen open/close
        }
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
            EIPacketHandler.sendToServer(new SelectAuraC2SPacket(""));
        } else {
            currentSelectedAuraId = preset.id();
            EIPacketHandler.sendToServer(new SelectAuraC2SPacket(preset.id()));
        }
    }
}

package com.crims.effectiveinstruments.compat.genshin.client;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.compat.genshin.GenshinInstrumentsCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Reflection bridge to Genshin Instruments' {@code InstrumentScreen}. Lets
 * the client overlay decide "is this a GI screen?" and read the screen's
 * instrument id without holding a compile-time reference to the GI class —
 * required for v1.5.0's optional-backend stance.
 *
 * <p>Both the {@link Class} lookup and the {@code getInstrumentId} method
 * lookup are <b>lazy</b>: the first call to either {@code is*} or
 * {@code get*} resolves them. A failed resolution sets
 * {@link #resolutionAttempted} so subsequent calls short-circuit silently
 * — without this, every {@code ScreenEvent.Init.Post} would re-attempt the
 * lookup, and a single failure would log on every screen open.
 *
 * <p>This class imports no GI types. The fully-qualified class name is a
 * string literal; reflection invocation never names the class at compile time.
 */
@OnlyIn(Dist.CLIENT)
public final class GenshinInstrumentScreenBridge {

    private static final String INSTRUMENT_SCREEN_CLASS =
            "com.cstav.genshinstrument.client.gui.screen.instrument.partial.InstrumentScreen";
    private static final String GET_INSTRUMENT_ID_METHOD = "getInstrumentId";

    @Nullable
    private static Class<?> screenClass;
    @Nullable
    private static Method getInstrumentIdMethod;
    private static boolean resolutionAttempted = false;
    private static boolean methodResolutionAttempted = false;

    public static boolean isInstrumentScreen(Screen screen) {
        if (!GenshinInstrumentsCompat.isAvailable()) return false;
        Class<?> clazz = resolveScreenClass();
        return clazz != null && clazz.isInstance(screen);
    }

    @Nullable
    public static ResourceLocation getInstrumentId(Screen screen) {
        if (!isInstrumentScreen(screen)) return null;
        Method method = resolveGetInstrumentIdMethod();
        if (method == null) return null;
        try {
            Object result = method.invoke(screen);
            return result instanceof ResourceLocation id ? id : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Failed to read Genshin instrument id from screen {}",
                    screen.getClass().getName(), ex);
            return null;
        }
    }

    @Nullable
    private static Class<?> resolveScreenClass() {
        if (resolutionAttempted) return screenClass;
        resolutionAttempted = true;
        try {
            screenClass = Class.forName(INSTRUMENT_SCREEN_CLASS);
        } catch (ClassNotFoundException ex) {
            // GI is supposedly available (compat said so) but the screen class
            // is missing — likely a version mismatch. Log once at warn so it's
            // visible without spamming.
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Genshin Instruments compat reports as available, but {} is not on the classpath. " +
                            "Stationary aura overlay disabled.",
                    INSTRUMENT_SCREEN_CLASS);
            screenClass = null;
        }
        return screenClass;
    }

    @Nullable
    private static Method resolveGetInstrumentIdMethod() {
        if (methodResolutionAttempted) return getInstrumentIdMethod;
        methodResolutionAttempted = true;
        Class<?> clazz = resolveScreenClass();
        if (clazz == null) return null;
        try {
            getInstrumentIdMethod = clazz.getMethod(GET_INSTRUMENT_ID_METHOD);
        } catch (NoSuchMethodException ex) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "Genshin InstrumentScreen no longer exposes getInstrumentId(); " +
                            "stationary aura overlay disabled for that screen.", ex);
            getInstrumentIdMethod = null;
        }
        return getInstrumentIdMethod;
    }

    private GenshinInstrumentScreenBridge() {}
}

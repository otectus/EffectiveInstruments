package com.crims.effectiveinstruments.compat.pehkui;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Pehkui reflection helper. Reads the entity's {@code BASE} scale via the
 * Pehkui API:
 * <pre>
 *   ScaleTypes.BASE.getScaleData(entity).getScale()
 * </pre>
 * Verified against {@code virtuoel/pehkui} branch {@code forge/1.20.1}:
 * {@code ScaleTypes.BASE} is a public static field on
 * {@code virtuoel.pehkui.api.ScaleTypes}, {@code ScaleType.getScaleData(Entity)}
 * returns {@code ScaleData}, and {@code ScaleData.getScale()} returns float.
 *
 * <p>If any reflection step fails (mod not loaded, API drift), the helper
 * returns {@code 1.0f} so the aura radius is unchanged.
 */
final class PehkuiReflection {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    @Nullable private static volatile Object scaleTypeBase;
    @Nullable private static volatile MethodHandle getScaleDataMH;
    @Nullable private static volatile MethodHandle getScaleMH;
    private static volatile boolean resolved = false;
    private static volatile boolean active = false;

    static synchronized boolean tryResolve() {
        if (resolved) return active;
        resolved = true;
        try {
            Class<?> scaleTypes = Class.forName("virtuoel.pehkui.api.ScaleTypes");
            Field baseField = scaleTypes.getField("BASE");
            scaleTypeBase = baseField.get(null);

            Class<?> scaleType = Class.forName("virtuoel.pehkui.api.ScaleType");
            Method getScaleData = scaleType.getMethod("getScaleData", Entity.class);
            getScaleData.setAccessible(true);
            getScaleDataMH = LOOKUP.unreflect(getScaleData);

            Class<?> scaleData = Class.forName("virtuoel.pehkui.api.ScaleData");
            Method getScale = scaleData.getMethod("getScale");
            getScale.setAccessible(true);
            getScaleMH = LOOKUP.unreflect(getScale);

            active = true;
            EffectiveInstrumentsMod.LOGGER.info("EI Pehkui adapter resolved: BASE scale lookup ready");
            return true;
        } catch (Throwable t) {
            EffectiveInstrumentsMod.LOGGER.warn(
                    "EI Pehkui adapter: reflection failed ({}); radius scaling disabled", t.getMessage());
            return false;
        }
    }

    /** Get the entity's BASE scale, or 1.0 if Pehkui isn't loaded or the lookup fails. */
    static float getBaseScale(Entity entity) {
        if (!active || scaleTypeBase == null || getScaleDataMH == null || getScaleMH == null) return 1.0f;
        try {
            Object data = getScaleDataMH.invoke(scaleTypeBase, entity);
            if (data == null) return 1.0f;
            return (float) getScaleMH.invoke(data);
        } catch (Throwable t) {
            return 1.0f;
        }
    }

    private PehkuiReflection() {}
}

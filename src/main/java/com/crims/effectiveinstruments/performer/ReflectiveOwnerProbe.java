package com.crims.effectiveinstruments.performer;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import net.minecraft.world.entity.LivingEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Last-resort owner-UUID probe used by {@link OwnerResolver} when no
 * registered {@link OwnerProvider} matches. Scans the entity class for a
 * zero-argument {@code getOwnerUUID() / getOwnerId() / getSummoner()} method
 * returning {@link UUID} or {@code Optional<UUID>}, then caches the resolved
 * {@link MethodHandle} per {@code Class<?>}.
 *
 * <p>NBT-backed owner fields (zombie thralls, custom mob "Owner" tags) are
 * the responsibility of each mod's own {@link OwnerProvider} — they need
 * NBT access via the mod's package-private save methods, which we can't
 * reach generically.
 *
 * <p>Failures are cached (as {@link #MISS}) so repeated lookups for a class
 * with no matching method short-circuit without log spam.
 */
final class ReflectiveOwnerProbe {

    private static final Object MISS = new Object();
    private static final ConcurrentHashMap<Class<?>, Object> CACHE = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    static Optional<UUID> tryRead(LivingEntity e) {
        Object cached = CACHE.computeIfAbsent(e.getClass(), ReflectiveOwnerProbe::resolveStrategy);
        if (cached == MISS) return Optional.empty();
        MethodHandle mh = (MethodHandle) cached;
        try {
            Object result = mh.invoke(e);
            if (result instanceof UUID u) return Optional.of(u);
            if (result instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof UUID u2) {
                return Optional.of(u2);
            }
            return Optional.empty();
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private static Object resolveStrategy(Class<?> cls) {
        Method m = findOwnerMethod(cls);
        if (m == null) return MISS;
        try {
            m.setAccessible(true);
            return LOOKUP.unreflect(m).asType(MethodType.methodType(Object.class, LivingEntity.class));
        } catch (Throwable t) {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "EI ReflectiveOwnerProbe: method present on {} but unreflect failed: {}",
                    cls.getName(), t.getMessage());
            return MISS;
        }
    }

    private static Method findOwnerMethod(Class<?> cls) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() != 0) continue;
                String name = m.getName();
                if (("getOwnerUUID".equals(name) || "getOwnerId".equals(name)
                        || "getSummoner".equals(name))
                        && (m.getReturnType() == UUID.class
                            || m.getReturnType() == Optional.class)) {
                    return m;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private ReflectiveOwnerProbe() {}
}

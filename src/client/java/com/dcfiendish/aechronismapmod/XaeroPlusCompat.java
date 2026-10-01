package com.dcfiendish.aechronismapmod;

import xaeroplus.feature.render.DrawFeature;
import xaeroplus.feature.render.DrawFeatureFactory;
import xaeroplus.feature.render.text.TextSupplier;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Bridges the few XaeroPlus API differences across its Minecraft 1.21.11 releases
 * (2.29.2 through 2.36.x), so one build of this mod runs against all of them.
 *
 * Async text features: XaeroPlus 2.32.0 renamed
 * DrawFeatureFactory.text(String, TextSupplier, int) to asyncText(...) with the same
 * signature and behavior. Calling either directly would throw NoSuchMethodError on the
 * other half of the range, so the factory method is looked up once at runtime.
 */
public final class XaeroPlusCompat {
    private static final MethodHandle ASYNC_TEXT = findAsyncText();

    private XaeroPlusCompat() {}

    public static DrawFeature asyncText(String id, TextSupplier textSupplier, int refreshIntervalMs) {
        try {
            return (DrawFeature) ASYNC_TEXT.invokeExact(id, textSupplier, refreshIntervalMs);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static MethodHandle findAsyncText() {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodType type = MethodType.methodType(DrawFeature.class, String.class, TextSupplier.class, int.class);
        try {
            return lookup.findStatic(DrawFeatureFactory.class, "asyncText", type); // 2.32.0+
        } catch (NoSuchMethodException | IllegalAccessException ignored) {}
        try {
            return lookup.findStatic(DrawFeatureFactory.class, "text", type); // 2.29.2 - 2.31.x
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Unsupported XaeroPlus version: no async text DrawFeatureFactory method", e);
        }
    }
}

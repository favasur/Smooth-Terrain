package io.github.favasur.smoothterrain.platform;

import java.util.ServiceLoader;

public final class PlatformLoader {
    private PlatformLoader() {
    }

    public static <T> T load(Class<T> type) {
        return ServiceLoader.load(type).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing Smooth Terrain platform service: " + type.getName()));
    }
}

package io.github.favasur.smoothterrain.fabric;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import io.github.favasur.smoothterrain.platform.IMixinPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Set;
import java.util.stream.Collectors;

public final class MixinPlatform implements IMixinPlatform {
    @Override
    public Set<String> getLoadedModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(container -> container.getMetadata().getId())
                .collect(Collectors.toSet());
    }

    @Override
    public void onLoad() {
        MixinExtrasBootstrap.init();
    }
}

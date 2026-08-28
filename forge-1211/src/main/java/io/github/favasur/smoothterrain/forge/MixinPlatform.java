package io.github.favasur.smoothterrain.forge;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import io.github.favasur.smoothterrain.platform.IMixinPlatform;
import net.minecraftforge.fml.ModList;

import java.util.Set;
import java.util.stream.Collectors;

public final class MixinPlatform implements IMixinPlatform {
    @Override
    public Set<String> getLoadedModIds() {
        return ModList.get().getMods().stream().map(mod -> mod.getModId()).collect(Collectors.toSet());
    }

    @Override
    public void onLoad() {
        MixinExtrasBootstrap.init();
    }
}

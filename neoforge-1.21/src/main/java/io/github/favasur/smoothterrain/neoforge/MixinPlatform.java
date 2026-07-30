package io.github.favasur.smoothterrain.neoforge;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import io.github.favasur.smoothterrain.platform.IMixinPlatform;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import java.util.Set;
import java.util.stream.Collectors;

public class MixinPlatform implements IMixinPlatform {
	@Override
	public Set<String> getLoadedModIds() {
		return LoadingModList.get().getMods().stream().map(ModInfo::getModId).collect(Collectors.toSet());
	}

	@Override
	public void onLoad() {
		MixinExtrasBootstrap.init();
	}
}

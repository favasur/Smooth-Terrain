package io.github.favasur.smoothterrain.fabric;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class Init implements ModInitializer {
	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register((event) -> {
			SmoothTerrainConfigImpl.loadServerConfig();
		});
		// TODO: Register network packets using Fabric API 1.21.x CustomPayload
	}
}

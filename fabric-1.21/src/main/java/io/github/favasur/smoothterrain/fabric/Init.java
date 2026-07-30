package io.github.favasur.smoothterrain.fabric;

import net.fabricmc.api.ModInitializer;

public class Init implements ModInitializer {
	@Override
	public void onInitialize() {
		// TODO: Load server config (SmoothTerrainConfigImpl references Minecraft
		// classes like Vec3i that Fabric Loom 1.7.x can't remap from shared source)
		// TODO: Register network packets using Fabric API 1.21.x CustomPayload
	}
}

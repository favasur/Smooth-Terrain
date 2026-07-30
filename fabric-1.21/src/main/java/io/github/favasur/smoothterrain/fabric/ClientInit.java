package io.github.favasur.smoothterrain.fabric;

import net.fabricmc.api.ClientModInitializer;

public class ClientInit implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// TODO: Register keybindings (KeyMapping class can't be resolved from shared
		// forge-1.21 source due to Fabric Loom 1.7.x remapping differences)
		// TODO: Register network packet handlers using Fabric API 1.21.x CustomPayload
	}
}

package io.github.favasur.smoothterrain.fabric;

import io.github.favasur.smoothterrain.client.KeyMappings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientInit implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// Minecraft 1.21 auto-registers keybindings via the KeyMapping constructor;
		// KeyBindingHelper.registerKeyBinding was removed in Fabric API 0.116+.
		KeyMappings.register(
			keyMapping -> {}, // no-op: auto-registered by vanilla
			keyBindingsOnTick -> ClientTickEvents.START_CLIENT_TICK.register(client -> keyBindingsOnTick.run())
		);
		// TODO: Register network packet handlers using Fabric API 1.21.x CustomPayload
	}
}

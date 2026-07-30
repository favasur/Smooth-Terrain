package io.github.favasur.smoothterrain.fabric;

import io.github.favasur.smoothterrain.client.KeyMappings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class ClientInit implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		KeyMappings.register(
			KeyBindingHelper::registerKeyBinding,
			keyBindingsOnTick -> ClientTickEvents.START_CLIENT_TICK.register(client -> keyBindingsOnTick.run())
		);
		// TODO: Register network packet handlers using Fabric API 1.21.x CustomPayload
	}
}

package io.github.favasur.smoothterrain.fabric;


import io.github.favasur.smoothterrain.client.KeyMappings;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkClient;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkFabric;
import io.github.favasur.smoothterrain.network.S2CUpdateServerConfig;
import io.github.favasur.smoothterrain.network.S2CUpdateSmoothable;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.concurrent.CompletableFuture;

public class ClientInit implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		KeyMappings.register(
			KeyBindingHelper::registerKeyBinding,
			keyBindingsOnTick -> ClientTickEvents.START_CLIENT_TICK.register(client -> keyBindingsOnTick.run())
		);
		ClientLoginNetworking.registerGlobalReceiver(S2CUpdateServerConfig.TYPE.getId(), (client, handler, buf, listenerAdder) -> {
			SmoothTerrainNetworkFabric.handleS2CUpdateServerConfigDuringLogin(client::execute, buf);
			return CompletableFuture.completedFuture(new FriendlyByteBuf(Unpooled.buffer()));
		});
		ClientPlayNetworking.registerGlobalReceiver(S2CUpdateServerConfig.TYPE, (packet, player, responseSender) -> {
			SmoothTerrainNetworkClient.handleS2CUpdateServerConfig(Minecraft.getInstance()::execute, packet.data());
		});
		ClientPlayNetworking.registerGlobalReceiver(S2CUpdateSmoothable.TYPE, (packet, player, responseSender) -> {
			SmoothTerrainNetworkClient.handleS2CUpdateSmoothable(Minecraft.getInstance()::execute, packet.newValue(), packet.states());
		});
	}
}

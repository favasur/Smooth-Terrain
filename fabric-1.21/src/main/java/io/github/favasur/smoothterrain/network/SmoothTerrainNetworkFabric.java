package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Consumer;
import java.util.function.Function;

public class SmoothTerrainNetworkFabric {

	/**
	 * @see #handleS2CUpdateServerConfigDuringLogin
	 */
	public static <T> T createS2CUpdateServerConfigDuringLogin(Function<byte[], T> constructor) {
		return constructor.apply(SmoothTerrainConfigImpl.readConfigFileBytes());
	}

	/**
	 * SmoothTerrain needs to know if the server it is connecting to has SmoothTerrain installed.
	 * This is because some features (collisions) require the mod to be installed on the server as well as the client.
	 * This packet lets us know that the mod is installed on the server - if we don't receive it, the mod isn't installed.
	 */
	public static void handleS2CUpdateServerConfigDuringLogin(Consumer<Runnable> enqueueWork, FriendlyByteBuf buf) {
		SmoothTerrainNetworkClient.currentServerHasSmoothTerrain = true;
		SmoothTerrainNetworkClient.handleS2CUpdateServerConfig(enqueueWork, SmoothTerrainNetwork.Serializer.decodeS2CUpdateServerConfig(buf, Function.identity()));
	}
}

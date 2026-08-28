package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.client.ClientUtil;
import io.github.favasur.smoothterrain.client.KeyMappings;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

import static io.github.favasur.smoothterrain.client.RenderHelper.reloadAllChunks;

public class SmoothTerrainNetworkClient {

	private static final Logger LOG = LogManager.getLogger();
	/**
	 * Only valid when connected to a server on the client.
	 * Contains random values from the most recently pinged server otherwise.
	 * Also valid for singleplayer integrated servers (always true).
	 */
	public static boolean currentServerHasSmoothTerrain = false;

	public static void handleS2CUpdateServerConfig(Consumer<Runnable> enqueueWork, byte[] configData) {
		ClientUtil.platform.receiveSyncedServerConfig(configData);
	}

	public static void handleS2CUpdateSmoothable(Consumer<Runnable> enqueueWork, boolean newValue, BlockState[] states) {
		enqueueWork.accept(() -> {
			SmoothTerrain.smoothableHandler.setSmoothable(newValue, states);
			reloadAllChunks("the server told us that the smoothness of some states changed");
		});
	}

	public static void onJoinedServer(boolean forgeAlreadyLoadedDefaultConfig) {
		LOG.debug("Client joined server");
		loadDefaultServerConfigIfWeAreOnAModdedServerWithoutSmoothTerrain(forgeAlreadyLoadedDefaultConfig);
		ClientUtil.sendPlayerInfoMessage();
		ClientUtil.warnPlayerIfVisualsDisabled();
		if (!currentServerHasSmoothTerrain) {
			// This lets players not phase through the ground on servers that don't have SmoothTerrain installed
			SmoothTerrainConfig.Server.collisionsEnabled = false;
			ClientUtil.warnPlayer(SmoothTerrain.MOD_ID + ".notification.notInstalledOnServer", KeyMappings.translate(KeyMappings.TOGGLE_SMOOTHABLE_BLOCK_TYPE));
		}
	}

	/**
	 * This lets SmoothTerrain load properly on modded servers that don't have it installed
	 */
	private static void loadDefaultServerConfigIfWeAreOnAModdedServerWithoutSmoothTerrain(boolean forgeAlreadyLoadedDefaultConfig) {
		if (currentServerHasSmoothTerrain) {
			// Forge has synced the server config to us, no need to load the default (see ConfigSync.syncConfigs)
			LOG.debug("Not loading default server config - current server has SmoothTerrain installed");
			return;
		}

		if (forgeAlreadyLoadedDefaultConfig) {
			// Forge has already loaded the default server configs for us (see NetworkHooks#handleClientLoginSuccess(Connection))
			LOG.debug("Not loading default server config - Forge has already loaded it for us");
			return;
		}

		LOG.debug("Connected to a modded server that doesn't have SmoothTerrain installed, loading default server config");
		ClientUtil.platform.loadDefaultServerConfig();
	}
}

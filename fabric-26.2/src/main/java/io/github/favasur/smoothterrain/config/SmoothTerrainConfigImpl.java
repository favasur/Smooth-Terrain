package io.github.favasur.smoothterrain.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

// TODO: REPLACE WITH AN ACTUAL CONFIG SYSTEM
/**
 * @see SmoothTerrainConfig
 */
public final class SmoothTerrainConfigImpl {

	public static void updateServerConfigSmoothable(boolean newValue, BlockState[] states) {
		var whitelist = new ArrayList<String>();
		var blacklist = new ArrayList<String>();
		SmoothTerrainConfig.Smoothables.updateUserDefinedSmoothableStringLists(newValue, states, whitelist, blacklist);
		SmoothTerrainConfig.Smoothables.recomputeInMemoryLookup(BuiltInRegistries.BLOCK.stream(), whitelist, blacklist, true);
	}

	public static void loadServerConfig() {
		loadDummyServerConfig();
	}

	public static void loadDefaultServerConfig() {
		loadDummyServerConfig();
	}

	public static void loadDummyServerConfig() {
		SmoothTerrainConfig.Common.debugEnabled = true;
		SmoothTerrainConfig.Client.render = true;
		SmoothTerrainConfig.Client.renderSelectionBox = true;
		SmoothTerrainConfig.Client.selectionBoxColor = new ColorParser.Color(0, 0, 0, 0x66).toRenderableColor();
		SmoothTerrainConfig.Server.mesher = SmoothTerrainConfig.Server.MesherType.SurfaceNets.instance;
		SmoothTerrainConfig.Server.collisionsEnabled = true;
		SmoothTerrainConfig.Server.tempMobCollisionsDisabled = true;
		SmoothTerrainConfig.Server.extendFluidsRange = 3;
		updateServerConfigSmoothable(true, new BlockState[0]);
	}

	public static byte[] readConfigFileBytes() {
		return new byte[0];
	}

	public static void receiveSyncedServerConfig(byte[] configData) {
		// TODO: Actually use the data
		assert configData.length == 0; // Since we are just debugging, and have no config system yet
		loadDummyServerConfig();
	}

	/**
	 * Implementation of {@link SmoothTerrainConfig.Common}
	 */
	public static class Common {
	}

	/**
	 * Implementation of {@link SmoothTerrainConfig.Client}
	 */
	public static class Client {
		public static void updateRender(boolean render) {
			SmoothTerrainConfig.Client.render = render;
		}
	}

	/**
	 * Implementation of {@link SmoothTerrainConfig.Server}
	 */
	public static class Server {
	}

}

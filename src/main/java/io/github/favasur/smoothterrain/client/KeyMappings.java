package io.github.favasur.smoothterrain.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetwork;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkClient;
import io.github.favasur.smoothterrain.util.ModUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

import static io.github.favasur.smoothterrain.client.RenderHelper.reloadAllChunks;

public final class KeyMappings {

	private static final Logger LOG = LogManager.getLogger();

	public static final String TOGGLE_VISUALS = "toggleVisuals";
	/**
	 * It is tedious to have to add/remove each block state for blocks that have many states with little to no visual difference.
	 * Leaves are a primary example of this.
	 * Therefore, the default add/remove keybind changes all block states for the targeted block.
	 * We have {@link #TOGGLE_SMOOTHABLE_BLOCK_STATE a separate keybind} that only changes a single state to give players that want it more fine-grained control.
	 */
	public static final String TOGGLE_SMOOTHABLE_BLOCK_TYPE = "toggleSmoothable";
	public static final String TOGGLE_SMOOTHABLE_BLOCK_STATE = "toggleSmoothableBlockState";

	public static void register(Consumer<KeyMapping> registerKey, Consumer<Runnable> registerClientTickHandler) {
		LOG.debug("Registering keybindings");
		var keybindings = Lists.newArrayList(
			makeKeybinding(registerKey, TOGGLE_VISUALS, InputConstants.UNKNOWN.getValue(), KeyMappings::toggleVisuals),
			makeKeybinding(registerKey, TOGGLE_SMOOTHABLE_BLOCK_TYPE, GLFW.GLFW_KEY_N, () -> toggleLookedAtSmoothable(true)),
			makeKeybinding(registerKey, TOGGLE_SMOOTHABLE_BLOCK_STATE, InputConstants.UNKNOWN.getValue(), () -> toggleLookedAtSmoothable(false))
		);
		registerClientTickHandler.accept(() -> {
			for (var keybinding : keybindings)
				if (keybinding.getKey().consumeClick()) {
					LOG.debug("Keybinding {} pressed", keybinding.getKey().getName());
					keybinding.getValue().run();
				}
		});
	}

	private static Pair<KeyMapping, Runnable> makeKeybinding(Consumer<KeyMapping> registerKey, String name, int key, Runnable action) {
		LOG.debug("Registering keybinding {}", name);
		var mapping = new KeyMapping(qualifyName(name), key, SmoothTerrain.MOD_ID + ".keycategory");
		registerKey.accept(mapping);
		return Pair.of(mapping, action);
	}

	private static String qualifyName(String name) {
		return SmoothTerrain.MOD_ID + ".key." + name;
	}

	public static Component translate(String name) {
		return Component.keybind(qualifyName(name));
	}

	private static void toggleVisuals() {
		if (SmoothTerrainConfig.Client.render && SmoothTerrainConfig.Server.forceVisuals) {
			ClientUtil.warnPlayer(SmoothTerrain.MOD_ID + ".notification.visualsForcedByServer");
			return;
		}
		ClientUtil.platform.updateClientVisuals(!SmoothTerrainConfig.Client.render);
		ClientUtil.warnPlayerIfVisualsDisabled();
		reloadAllChunks("toggleVisuals was pressed");
	}

	private static void toggleLookedAtSmoothable(boolean changeAllStatesOfBlock) {
		var minecraft = Minecraft.getInstance();
		var world = minecraft.level;
		var player = minecraft.player;
		var lookingAt = minecraft.hitResult;
		if (world == null || player == null || lookingAt == null || lookingAt.getType() != HitResult.Type.BLOCK) {
			LOG.debug("toggleLookedAtSmoothable preconditions not met (world={}, player={}, lookingAt={})", world, player, lookingAt);
			return;
		}

		var targeted = ((BlockHitResult) lookingAt);
		var targetedState = world.getBlockState(targeted.getBlockPos());
		var newValue = !SmoothTerrain.smoothableHandler.isSmoothable(targetedState);
		var states = changeAllStatesOfBlock ? ModUtil.getStates(targetedState.getBlock()).toArray(BlockState[]::new) : new BlockState[] {targetedState};

		LOG.debug("toggleLookedAtSmoothable currentServerHasSmoothTerrain={}", SmoothTerrainNetworkClient.currentServerHasSmoothTerrain);
		if (!SmoothTerrainNetworkClient.currentServerHasSmoothTerrain) {
			// The server doesn't have SmoothTerrain, directly modify the smoothable state to hackily allow the player to have visuals
			SmoothTerrain.smoothableHandler.setSmoothable(newValue, states);
			reloadAllChunks("toggleLookedAtSmoothable was pressed while connected to a server that doesn't have SmoothTerrain installed");
		} else {
			// We're on a server (possibly singleplayer) with SmoothTerrain installed
			if (SmoothTerrainNetwork.checkPermissionAndNotifyIfUnauthorised(player, Minecraft.getInstance().getSingleplayerServer()))
				// Only send the packet if we have permission, don't send a packet that will be denied
				ClientUtil.platform.sendC2SRequestUpdateSmoothable(newValue, states);
		}
	}

}

package io.github.favasur.smoothterrain;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.smoothable.SmoothableHandler;
import io.github.favasur.smoothterrain.util.ModUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author Cadiboo
 */
public final class SmoothTerrain {

	public static final String MOD_ID = "smoothterrain";
	public static final SmoothableHandler smoothableHandler = SmoothableHandler.create();

	// region API
	/**
	 * For other mods.
	 * Check if a block is smoothable in-world (takes user/server configuration into account).
	 */
	public static boolean isSmoothable(BlockState state) {
		return smoothableHandler.isSmoothable(state);
	}

	/**
	 * For other mods.
	 * Add your block(s) as being smoothable 'by default' (may be overridden in world by user/server config).
	 */
	public static void addSmoothable(Block... blocks) {
		for (var block : blocks)
			addSmoothable(ModUtil.getStates(block).toArray(new BlockState[0]));
	}

	/**
	 * For other mods.
	 * Add your block(s) as being smoothable 'by default' (may be overridden in world by user/server config).
	 */
	public static void addSmoothable(BlockState... states) {
		SmoothTerrainConfig.Smoothables.addDefault(states);
	}
	// endregion

}

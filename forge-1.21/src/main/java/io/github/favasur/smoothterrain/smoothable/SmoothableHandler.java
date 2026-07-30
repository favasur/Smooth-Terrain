package io.github.favasur.smoothterrain.smoothable;

import io.github.favasur.smoothterrain.hooks.trait.ISmoothTerrainBlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;

/**
 * The in-memory list of smoothables.
 * Shared between client and server in singleplayer.
 * Uses the {@link io.github.favasur.smoothterrain.mixin.BlockStateBaseMixin#noCubes$isSmoothable} field which is added via ASM at runtime.
 *
 * @author Cadiboo
 */
public interface SmoothableHandler {

	boolean isSmoothable(BlockStateBase state);

	void setSmoothable(boolean newValue, BlockStateBase state);

	default void setSmoothable(boolean newValue, BlockStateBase[] states) {
		for (var state : states)
			setSmoothable(newValue, state);
	}

	static SmoothableHandler create() {
		return new SmoothableHandler() {
			@Override
			public boolean isSmoothable(BlockStateBase state) {
				return ((ISmoothTerrainBlockState) state).noCubes$isSmoothable();
			}

			@Override
			public void setSmoothable(boolean newValue, BlockStateBase state) {
				((ISmoothTerrainBlockState) state).noCubes$setSmoothable(newValue);
			}
		};
	}


}

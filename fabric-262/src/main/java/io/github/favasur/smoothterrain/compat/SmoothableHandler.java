package io.github.favasur.smoothterrain.smoothable;

import io.github.favasur.smoothterrain.hooks.trait.ISmoothTerrainBlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;

public interface SmoothableHandler {
    boolean isSmoothable(BlockStateBase state);
    void setSmoothable(boolean value, BlockStateBase state);

    default void setSmoothable(boolean value, BlockStateBase[] states) {
        for (BlockStateBase state : states) {
            setSmoothable(value, state);
        }
    }

    static SmoothableHandler create() {
        return new SmoothableHandler() {
            @Override
            public boolean isSmoothable(BlockStateBase state) {
                return state instanceof ISmoothTerrainBlockState smoothable && smoothable.noCubes$isSmoothable();
            }

            @Override
            public void setSmoothable(boolean value, BlockStateBase state) {
                if (state instanceof ISmoothTerrainBlockState smoothable) {
                    smoothable.noCubes$setSmoothable(value);
                }
            }
        };
    }
}

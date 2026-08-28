package io.github.favasur.smoothterrain.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface IPlatform {
    Identifier getRegistryName(Block block);
    boolean isPlant(BlockState state);
    void updateServerConfigSmoothable(boolean newValue, BlockState... states);

    /**
     * Whether the block renders as a full opaque solid. Smooth surfaces clamp to such blocks
     * (they are treated as fully inside the density field) and their boundary faces are skipped
     * to avoid z-fighting with their vanilla faces. The signature differs between 1.21.1
     * ({@code isSolidRender(BlockGetter, BlockPos)}) and 26.2 ({@code isSolidRender()}), so it is
     * abstracted per loader.
     */
    default boolean isSolidRender(BlockState state) {
        return state.isSolidRender();
    }
}

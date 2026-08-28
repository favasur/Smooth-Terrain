package io.github.favasur.smoothterrain.hooks.trait;

/**
 * Adds extra functionality to {@link net.minecraft.world.level.block.state.BlockState}.
 * Implemented by {@link io.github.favasur.smoothterrain.mixin.BlockStateBaseMixin}.
 * Inspired by <a href="https://forums.minecraftforge.net/topic/11596-add-a-field-to-a-base-class/?do=findComment&comment=61923">this post</a>.
 */
public interface ISmoothTerrainBlockState {

	void noCubes$setSmoothable(boolean value);

	boolean noCubes$isSmoothable();

}

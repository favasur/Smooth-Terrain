package io.github.favasur.smoothterrain.hooks.trait;

/**
 * Adds extra functionality to {@link net.minecraft.world.level.block.Block}.
 * Implemented by {@link io.github.favasur.smoothterrain.mixin.BlockMixin}.
 */
public interface ISmoothTerrainBlockType {
	boolean noCubes$hasCollision();
}

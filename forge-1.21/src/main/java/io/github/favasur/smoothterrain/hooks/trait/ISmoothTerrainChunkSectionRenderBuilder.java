package io.github.favasur.smoothterrain.hooks.trait;

import net.minecraft.core.BlockPos;

/**
 * Adds extra functionality to {@link net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk.RebuildTask}.
 * Implemented by {@link io.github.favasur.smoothterrain.mixin.client.RenderChunkRebuildTaskMixin}.
 */
public interface ISmoothTerrainChunkSectionRenderBuilder {
	/**
	 * ModelData only exists on Forge, so we use Object as the type here instead.
	 */
	Object noCubes$getModelData(BlockPos worldPos);
}

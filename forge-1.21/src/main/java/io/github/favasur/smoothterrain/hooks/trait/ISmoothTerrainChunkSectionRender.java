package io.github.favasur.smoothterrain.hooks.trait;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Adds extra functionality to {@link net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection}.
 * Implemented by {@link io.github.favasur.smoothterrain.mixin.client.RenderChunkMixin}.
 */
public interface ISmoothTerrainChunkSectionRender {
	void noCubes$beginLayer(VertexConsumer buffer);
}

package io.github.favasur.smoothterrain.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.favasur.smoothterrain.hooks.trait.ISmoothTerrainChunkSectionRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderChunkMixin implements ISmoothTerrainChunkSectionRender {

	@Shadow
	abstract void shadow$beginLayer(BufferBuilder buffer);

	@Override
	public void noCubes$beginLayer(VertexConsumer buffer) {
		if (buffer instanceof BufferBuilder bb) {
			shadow$beginLayer(bb);
		}
	}
}

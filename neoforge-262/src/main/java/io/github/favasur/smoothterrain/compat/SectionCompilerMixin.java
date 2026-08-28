package io.github.favasur.smoothterrain.compat;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.favasur.smoothterrain.client.render.VanillaRenderer;
import io.github.favasur.smoothterrain.hooks.ClientHooks;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Function;

/**
 * 26.2 counterpart of the 1.21.1 render mixins. Two changes to {@link SectionCompiler#compile}:
 * <ol>
 *   <li>Redirects {@code BlockState.getRenderShape()} through {@link ClientHooks#getRenderShape}
 *       so smoothable blocks are skipped by the vanilla per-block loop (the mesh renders them);</li>
 *   <li>Injects the smooth terrain mesh at the exact point the vanilla loop finishes and the
 *       section's buffers are about to be built into mesh data (the {@code Map#entrySet()} call
 *       that starts the build loop), handing the per-layer builders to
 *       {@link VanillaRenderer#renderChunk}.</li>
 * </ol>
 */
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {

	/**
	 * @see ClientHooks#getRenderShape
	 */
	@Redirect(
		method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"
		)
	)
	public RenderShape noCubes$getRenderShape(BlockState state) {
		return ClientHooks.getRenderShape(state);
	}

	/**
	 * The mesh vertices are in section-local coordinates (the mesher emits relative to the section
	 * origin), the same space the vanilla per-block geometry uses, so an identity {@link PoseStack}
	 * is correct - matching how the 26.2 compile passes relative coordinates to its tesselator.
	 */
	@Inject(
		method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/Map;entrySet()Ljava/util/Set;"
		)
	)
	private void renderSmoothTerrainMesh(
		SectionPos sectionPos, RenderSectionRegion region,
		VertexSorting vertexSorting, SectionBufferBuilderPack pack,
		CallbackInfo ci,
		@Local Map<ChunkSectionLayer, BufferBuilder> startedLayers
	) {
		Function<ChunkSectionLayer, VertexConsumer> bufferFactory = layer -> startedLayers.computeIfAbsent(
			layer,
			l -> new BufferBuilder(pack.buffer(l), PrimitiveTopology.QUADS, l.vertexFormat())
		);
		VanillaRenderer.renderChunk(sectionPos.origin(), new PoseStack(), region, bufferFactory);
	}

}

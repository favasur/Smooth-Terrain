package io.github.favasur.smoothterrain.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.favasur.smoothterrain.client.render.VanillaRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Function;

/**
 * Fabric's analogue of the NeoForge {@code AddSectionGeometryEvent}. NeoForge fires that event at
 * the exact point in {@link SectionCompiler#compile} where the vanilla per-block loop is done and
 * the section's buffers are about to be turned into {@code MeshData}; Fabric has no such event, so
 * we inject at the same point: the {@code Map#entrySet()} call that starts the build loop.
 * <p>
 * The {@code Map<RenderType, BufferBuilder>} local holds the section's per-layer builders (started
 * by {@code getOrBeginLayer} as vanilla rendered the non-smoothable blocks). Smoothable blocks were
 * skipped by the vanilla loop via the {@code getRenderShape} redirect in
 * {@link RenderChunkRebuildTaskMixin}, so the mesh renderer's buffer factory creates any missing
 * layer builders on demand, exactly like vanilla's own {@code getOrBeginLayer}.
 * <p>
 * Mesh vertices are in section-local coordinates (the mesher emits relative to the section origin),
 * which is the same space vanilla's per-block geometry uses, so the identity {@link PoseStack} of
 * the section compile is correct.
 */
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {

	@Inject(
		method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/Map;entrySet()Ljava/util/Set;"
		)
	)
	private void renderSmoothTerrainMesh(
		SectionPos sectionPos, RenderChunkRegion region,
		VertexSorting vertexSorting, SectionBufferBuilderPack pack,
		CallbackInfo ci,
		@Local PoseStack posestack,
		@Local Map<RenderType, BufferBuilder> map
	) {
		Function<RenderType, VertexConsumer> bufferFactory = type -> map.computeIfAbsent(
			type,
			t -> new BufferBuilder(pack.buffer(t), VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK)
		);
		VanillaRenderer.renderChunk(sectionPos.origin(), posestack, region, bufferFactory);
	}

}

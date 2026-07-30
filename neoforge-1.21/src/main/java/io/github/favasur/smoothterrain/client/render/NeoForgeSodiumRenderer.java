package io.github.favasur.smoothterrain.client.render;

import io.github.favasur.smoothterrain.client.render.struct.Color;
import io.github.favasur.smoothterrain.client.render.struct.FaceLight;
import io.github.favasur.smoothterrain.client.render.struct.Texture;
import io.github.favasur.smoothterrain.hooks.trait.ISmoothTerrainChunkSectionRenderBuilderSodium;
import io.github.favasur.smoothterrain.util.Face;
import io.github.favasur.smoothterrain.util.Vec;
import io.github.favasur.smoothterrain.client.render.MeshRenderer.MutableObjects;
import io.github.favasur.smoothterrain.client.render.MeshRenderer.FaceInfo;
import io.github.favasur.smoothterrain.client.render.MeshRenderer.ISmoothTerrainAreaRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import org.embeddedt.embeddium.api.render.chunk.BlockRenderContext;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BlockRenderCache;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.DefaultMaterials;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;

/**
 * Sodium/Embeddium compatibility renderer for NeoForge 1.21.x.
 * <p>
 * Ported from the old Sodium 0.5 API ({@code me.jellysquid.mods.sodium.*}) to
 * the Embeddium 1.21.1 API ({@code org.embeddedt.embeddium.*}).
 * <p>
 * Key changes from the old API:
 * <ul>
 *   <li>All packages renamed from {@code me.jellysquid.mods.sodium.*} to
 *       {@code org.embeddedt.embeddium.*}</li>
 *   <li>{@link BlockRenderContext#origin()} now returns immutable {@code Vector3fc} —
 *       block offset uses {@link BlockRenderContext#stack()} translate instead</li>
 *   <li>{@link BlockRenderContext#update} now takes typed {@link ModelData} instead of
 *       {@code Object}</li>
 *   <li>Getter methods replace direct field access on BlockRenderContext</li>
 * </ul>
 */
public final class NeoForgeSodiumRenderer {

	public static void renderChunk(
		ISmoothTerrainChunkSectionRenderBuilderSodium task,
		RandomSource random,
		Object buffersIn,
		Object cacheIn,
		BlockPos.MutableBlockPos chunkPos,
		BlockPos.MutableBlockPos modelOffset,
		Object contextIn
	) {
		if (MeshRenderer.shouldSkipChunkMeshRendering())
			return;

		var vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
		var buffers = (ChunkBuildBuffers) buffersIn;
		var cache = (BlockRenderCache) cacheIn;
		var context = (BlockRenderContext) contextIn;

		var objects = MutableObjects.INSTANCE.get();
		var world = cache.getWorldSlice();
		var blockColors = Minecraft.getInstance().getBlockColors();
		MeshRenderer.renderChunk(
			world, chunkPos,
			new ISmoothTerrainAreaRenderer() {
				@Override
				public void quad(BlockState state, BlockPos worldPos, FaceInfo faceInfo, boolean renderBothSides, Color colorOverride, LightCache lightCache, float shade) {
					var light = lightCache.get(chunkPos, faceInfo.face, faceInfo.normal, objects.light);
					forEachQuadInEveryBlockLayer(
						task, buffers, cache, modelOffset, context, random,
						state, worldPos, faceInfo.approximateDirection,
						(colorState, colorWorldPos, quad) -> colorOverride != null ? colorOverride : VanillaRenderer.applyColorTo(
							world, blockColors,
							objects.color, quad, colorState, colorWorldPos, shade
						),
						(layer, material, buffer, quad, color, emissive) -> {
							var sprite = quad.getSprite();
							if (sprite != null) {
								buffer.addSprite(sprite);
							}

							var normal = faceInfo.normal;
							var isApproximatelyFlat = Math.abs(normal.x) >= 0.9 || Math.abs(normal.y) >= 0.9 || Math.abs(normal.z) >= 0.9;

							var vertexBuffer = buffer.getVertexBuffer(isApproximatelyFlat ? ModelQuadFacing.fromDirection(faceInfo.approximateDirection) : ModelQuadFacing.UNASSIGNED);

							var texture = Texture.forQuadRearranged(objects.texture, quad, faceInfo.approximateDirection);
							renderQuad(vertexBuffer, material, vertices, context, faceInfo, color, texture, emissive ? FaceLight.MAX_BRIGHTNESS : light, renderBothSides);
						}
					);
				}
				@Override
				public void block(BlockState stateIn, BlockPos worldPosIn, float relativeX, float relativeY, float relativeZ) {
					// Embeddium 1.21.1: origin() is immutable, use PoseStack for offset
					context.stack().pushPose();
					try {
						context.stack().translate(relativeX, relativeY, relativeZ);
						renderInBlockLayers(
							task, buffers, cache, modelOffset, context,
							stateIn, worldPosIn,
							(state, worldPos, model, seed, modelData, layer, material, buffer) -> {
								cache.getBlockRenderer().renderModel(context, buffers);
							}
						);
					} finally {
						context.stack().popPose();
					}
				}
			}
		);
	}

	public interface RenderInLayer {
		void render(BlockState state, BlockPos worldPos, BakedModel model, long seed, ModelData modelData, RenderType layer, Material material, ChunkModelBuilder buffer);
	}

	static void renderInBlockLayers(
		ISmoothTerrainChunkSectionRenderBuilderSodium task,
		ChunkBuildBuffers buffers,
		BlockRenderCache cache,
		BlockPos.MutableBlockPos modelOffset,
		BlockRenderContext context,
		BlockState state, BlockPos worldPos, RenderInLayer render
	) {
		var model = cache.getBlockModels().getBlockModel(state);
		var seed = state.getSeed(worldPos);
		ModelData modelData = (ModelData) task.noCubes$getModelData(worldPos);
		var layers = ItemBlockRenderTypes.getRenderLayers(state);
		for (var layer : layers) {
			context.update(worldPos, modelOffset, state, model, seed, modelData, layer);
			var material = DefaultMaterials.forRenderLayer(layer);
			var buffer = buffers.get(material);
			render.render(state, worldPos, model, seed, modelData, layer, material, buffer);
		}
	}

	public interface QuadConsumer {
		void accept(RenderType layer, Material material, ChunkModelBuilder buffer, BakedQuad quad, Color color, boolean emissive);
	}

	static void forEachQuadInEveryBlockLayer(
		ISmoothTerrainChunkSectionRenderBuilderSodium task,
		ChunkBuildBuffers buffers,
		BlockRenderCache cache,
		BlockPos.MutableBlockPos modelOffset,
		BlockRenderContext context,
		RandomSource random,
		BlockState stateIn, BlockPos worldPosIn, Direction direction, VanillaRenderer.ColorSupplier colorSupplier, QuadConsumer action
	) {
		renderInBlockLayers(
			task, buffers, cache, modelOffset, context,
			stateIn, worldPosIn,
			(state, worldPos, model, seed, modelData, layer, material, buffer) -> {
				MeshRenderer.forEveryQuadForState(
					state, model, direction,
					cache.getBlockModels(), random,
					(state1) -> cache.getBlockModels().getBlockModel(state1),
					(state1, model1, direction1) -> {
						random.setSeed(seed);
						return model1.getQuads(state1, direction1, random, modelData, layer);
					},
					(state1, quad) -> {
						var color = colorSupplier.apply(state1, worldPos, quad);
						action.accept(layer, material, buffer, quad, color, false);
					},
					(state1, model1) -> 0
				);
			}
		);
	}

	static void renderQuad(
		ChunkMeshBufferBuilder vertexBuffer, Material material,
		ChunkVertexEncoder.Vertex[] vertices, BlockRenderContext context,
		FaceInfo faceInfo,
		Color color,
		Texture uvs,
		FaceLight light,
		boolean renderBothSides
	) {
		quad(vertexBuffer, material, vertices, context, faceInfo.face, faceInfo.normal, color, uvs, light, renderBothSides);
	}

	static void quad(
		ChunkMeshBufferBuilder vertexBuffer, Material material,
		ChunkVertexEncoder.Vertex[] vertices, BlockRenderContext context,
		Face face, Vec faceNormal,
		Color color,
		Texture uvs,
		FaceLight light,
		boolean doubleSided
	) {
		quad(
			vertexBuffer, material,
			vertices, context, doubleSided,
			face, color, uvs, OverlayTexture.NO_OVERLAY, light, faceNormal
		);
	}

	static void quad(
		ChunkMeshBufferBuilder vertexBuffer, Material material,
		ChunkVertexEncoder.Vertex[] vertices, BlockRenderContext context, boolean doubleSided,
		Face face, Color color, Texture texture, int overlay, FaceLight light, Vec normal
	) {
		quad(
			vertexBuffer, material,
			vertices, context, doubleSided,
			face.v0, color, texture.u0, texture.v0, overlay, light.v0, normal,
			face.v1, color, texture.u1, texture.v1, overlay, light.v1, normal,
			face.v2, color, texture.u2, texture.v2, overlay, light.v2, normal,
			face.v3, color, texture.u3, texture.v3, overlay, light.v3, normal
		);
	}

	static void quad(
		ChunkMeshBufferBuilder vertexBuffer, Material material,
		ChunkVertexEncoder.Vertex[] vertices, BlockRenderContext context, boolean doubleSided,
		Vec vec0, Color color0, float u0, float v0, int overlay0, int light0, Vec normal0,
		Vec vec1, Color color1, float u1, float v1, int overlay1, int light1, Vec normal1,
		Vec vec2, Color color2, float u2, float v2, int overlay2, int light2, Vec normal2,
		Vec vec3, Color color3, float u3, float v3, int overlay3, int light3, Vec normal3
	) {
		quad(
			vertexBuffer, material,
			vertices, context, doubleSided,
			vec0.x, vec0.y, vec0.z, color0.red, color0.green, color0.blue, color0.alpha, u0, v0, overlay0, light0, normal0.x, normal0.y, normal0.z,
			vec1.x, vec1.y, vec1.z, color1.red, color1.green, color1.blue, color1.alpha, u1, v1, overlay1, light1, normal1.x, normal1.y, normal1.z,
			vec2.x, vec2.y, vec2.z, color2.red, color2.green, color2.blue, color2.alpha, u2, v2, overlay2, light2, normal2.x, normal2.y, normal2.z,
			vec3.x, vec3.y, vec3.z, color3.red, color3.green, color3.blue, color3.alpha, u3, v3, overlay3, light3, normal3.x, normal3.y, normal3.z
		);
		if (doubleSided) {
			quad(
				vertexBuffer, material, vertices, context, false,
				vec0.x, vec0.y, vec0.z, color0.red, color0.green, color0.blue, color0.alpha, u0, v0, overlay0, light0, normal0.x, normal0.y, normal0.z,
				vec3.x, vec3.y, vec3.z, color3.red, color3.green, color3.blue, color3.alpha, u3, v3, overlay3, light3, normal3.x, normal3.y, normal3.z,
				vec2.x, vec2.y, vec2.z, color2.red, color2.green, color2.blue, color2.alpha, u2, v2, overlay2, light2, normal2.x, normal2.y, normal2.z,
				vec1.x, vec1.y, vec1.z, color1.red, color1.green, color1.blue, color1.alpha, u1, v1, overlay1, light1, normal1.x, normal1.y, normal1.z
			);
		}
	}

	static void quad(
		ChunkMeshBufferBuilder vertexBuffer, Material material,
		ChunkVertexEncoder.Vertex[] vertices, BlockRenderContext context, boolean doubleSided,
		float v0x, float v0y, float v0z, float red0, float green0, float blue0, float alpha0, float u0, float v0, int overlay0, int light0, float n0x, float n0y, float n0z,
		float v1x, float v1y, float v1z, float red1, float green1, float blue1, float alpha1, float u1, float v1, int overlay1, int light1, float n1x, float n1y, float n1z,
		float v2x, float v2y, float v2z, float red2, float green2, float blue2, float alpha2, float u2, float v2, int overlay2, int light2, float n2x, float n2y, float n2z,
		float v3x, float v3y, float v3z, float red3, float green3, float blue3, float alpha3, float u3, float v3, int overlay3, int light3, float n3x, float n3y, float n3z
	) {
		vertex(vertices[0], context, v0x, v0y, v0z, red0, green0, blue0, alpha0, u0, v0, overlay0, light0, n0x, n0y, n0z);
		vertex(vertices[1], context, v1x, v1y, v1z, red1, green1, blue1, alpha1, u1, v1, overlay1, light1, n1x, n1y, n1z);
		vertex(vertices[2], context, v2x, v2y, v2z, red2, green2, blue2, alpha2, u2, v2, overlay2, light2, n2x, n2y, n2z);
		vertex(vertices[3], context, v3x, v3y, v3z, red3, green3, blue3, alpha3, u3, v3, overlay3, light3, n3x, n3y, n3z);
		vertexBuffer.push(vertices, material);
	}

	static void vertex(
		ChunkVertexEncoder.Vertex vertex, BlockRenderContext context,
		float vx, float vy, float vz,
		float red, float green, float blue, float alpha,
		float u, float v,
		int overlay,
		int light,
		float nx, float ny, float nz
	) {
		vertex.x = context.origin().x() + vx;
		vertex.y = context.origin().y() + vy;
		vertex.z = context.origin().z() + vz;
		vertex.color = ColorABGR.pack(red, green, blue, alpha);
		vertex.u = u;
		vertex.v = v;
		vertex.light = light;
	}
}

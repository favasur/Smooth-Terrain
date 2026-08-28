package io.github.favasur.smoothterrain.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.favasur.smoothterrain.client.ClientUtil;
import io.github.favasur.smoothterrain.client.render.struct.Color;
import io.github.favasur.smoothterrain.client.render.struct.FaceLight;
import io.github.favasur.smoothterrain.client.render.struct.Texture;
import io.github.favasur.smoothterrain.util.Face;
import io.github.favasur.smoothterrain.util.Vec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.function.Function;

import static io.github.favasur.smoothterrain.client.RenderHelper.vertex;

/**
 * Vanilla chunk renderer for SmoothTerrain 1.21.x.
 *
 * Instead of a mixin into {@code SectionCompiler.compile} (the old approach used @Local sugar to
 * grab the section's raw buffer map, which generated invalid bytecode and crashed the game), the
 * mesh is rendered through NeoForge's {@link AddSectionGeometryEvent}: it fires at exactly the
 * point in {@code SectionCompiler.compile} where the vanilla per-block loop is done and the
 * buffers are turned into {@code MeshData}, and its {@code SectionRenderingContext} hands us the
 * per-layer {@link VertexConsumer}s (bound to the section's buffer pack), the chunk region and the
 * pose stack - everything the mesh renderer needs. Smoothable blocks are skipped by the vanilla
 * loop via the getRenderShape redirect in {@code RenderChunkRebuildTaskMixin}.
 */
public final class VanillaRenderer {

	public static void renderChunk(BlockPos sectionOrigin, AddSectionGeometryEvent.SectionRenderingContext ctx) {
		if (MeshRenderer.shouldSkipChunkMeshRendering())
			return;

		PoseStack matrix = ctx.getPoseStack();
		BlockAndTintGetter world = ctx.getRegion();
		Function<RenderType, VertexConsumer> bufferFactory = ctx::getOrCreateChunkBuffer;
		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		RandomSource random = RandomSource.create();

		// Whole blocks the mesher keeps in place (short plants with fixPlantHeight) go through the
		// vanilla block pipeline with the same buffer/pose/light context as the section compile.
		RenderInLayer renderBlock = (state, worldPos, modelData, layer, buffer) ->
			dispatcher.renderBatched(state, worldPos, world, matrix, buffer, false, random,
				(ModelData) modelData, layer);

		matrix.pushPose();
		try {
			var objects = MeshRenderer.MutableObjects.INSTANCE.get();
			var blockColors = Minecraft.getInstance().getBlockColors();
			MeshRenderer.renderChunk(
				world, sectionOrigin,
				new MeshRenderer.ISmoothTerrainAreaRenderer() {
					@Override
					public void quad(BlockState state, BlockPos worldPos, MeshRenderer.FaceInfo faceInfo, boolean renderBothSides, Color colorOverride, LightCache lightCache, float shade) {
						var light = lightCache.get(sectionOrigin, faceInfo.face, faceInfo.normal, objects.light);
						forEachQuadInEveryBlockLayer(
							bufferFactory,
							world, matrix, random, dispatcher,
							state, worldPos, faceInfo.approximateDirection,
							(colorState, colorWorldPos, quad) -> colorOverride != null ? colorOverride : applyColorTo(
								world, blockColors,
								objects.color, quad, colorState, colorWorldPos, shade
							),
							(layer, buffer, quad, color, emissive) -> {
								var texture = Texture.forQuadRearranged(objects.texture, quad, faceInfo.approximateDirection);
								renderQuad(buffer, matrix, faceInfo, color, texture, emissive ? FaceLight.MAX_BRIGHTNESS : light, renderBothSides);
							}
						);
					}
					@Override
					public void block(BlockState state, BlockPos worldPos, float relativeX, float relativeY, float relativeZ) {
						matrix.pushPose();
						try {
							matrix.translate(relativeX, relativeY, relativeZ);
							renderInBlockLayers(
								bufferFactory,
								state, worldPos,
								renderBlock
							);
						} finally {
							matrix.popPose();
						}
					}
				}
			);
		} finally {
			matrix.popPose();
		}
	}

	public interface RenderInLayer {
		void render(BlockState state, BlockPos worldPos, Object modelData, RenderType layer, VertexConsumer buffer);
	}

	static void renderInBlockLayers(
		Function<RenderType, VertexConsumer> bufferFactory,
		BlockState state, BlockPos worldPos, RenderInLayer render
	) {
		// The event API does not expose per-position model data; ModelData.EMPTY is what vanilla
		// uses when no model data is registered, and this renderer never relied on custom data.
		var modelData = ModelData.EMPTY;
		ClientUtil.platform.forEachRenderLayer(state, layer -> {
			var buffer = bufferFactory.apply(layer);
			render.render(state, worldPos, modelData, layer, buffer);
		});
	}

	static Color applyColorTo(
		BlockAndTintGetter world, BlockColors blockColors,
		Color color, BakedQuad quad,
		BlockState state, BlockPos pos,
		float shade
	) {
		if (!quad.isTinted()) {
			color.red = shade;
			color.green = shade;
			color.blue = shade;
			return color;
		}
		int packedColor = blockColors.getColor(state, world, pos, quad.getTintIndex());
		color.red = (float) (packedColor >> 16 & 255) / 255.0F * shade;
		color.green = (float) (packedColor >> 8 & 255) / 255.0F * shade;
		color.blue = (float) (packedColor & 255) / 255.0F * shade;
		return color;
	}

	public interface ColorSupplier {
		Color apply(BlockState state, BlockPos worldPos, BakedQuad quad);
	}

	public interface QuadConsumer {
		void accept(RenderType layer, VertexConsumer buffer, BakedQuad quad, Color color, boolean emissive);
	}

	static void forEachQuadInEveryBlockLayer(
		Function<RenderType, VertexConsumer> bufferFactory,
		BlockAndTintGetter world, PoseStack matrix,
		RandomSource random, BlockRenderDispatcher dispatcher,
		BlockState stateIn, BlockPos worldPosIn, Direction direction,
		ColorSupplier colorSupplier, QuadConsumer action
	) {
		var seed = stateIn.getSeed(worldPosIn);
		renderInBlockLayers(
			bufferFactory,
			stateIn, worldPosIn,
			(state, worldPos, modelData, layer, buffer) -> {
				var model = dispatcher.getBlockModel(state);
				MeshRenderer.forEveryQuadForState(
					state, model, direction,
					dispatcher.getBlockModelShaper(), random,
					dispatcher::getBlockModel,
					(state1, model1, direction1) -> {
						random.setSeed(seed);
						return ClientUtil.platform.getQuads(model1, state1, direction1, random, modelData, layer);
					},
					(state1, quad) -> applyActionToQuad(
						buffer, quad,
						state1, worldPos, colorSupplier, layer, action
					),
					(state1, model1) -> 0
				);
			}
		);
	}

	public static void applyActionToQuad(
		VertexConsumer buffer, BakedQuad quad,
		BlockState state, BlockPos worldPos,
		ColorSupplier colorSupplier, RenderType layer,
		QuadConsumer action
	) {
		var color = colorSupplier.apply(state, worldPos, quad);
		action.accept(layer, buffer, quad, color, false);
	}

	public static void renderQuad(
		VertexConsumer buffer, PoseStack matrix,
		MeshRenderer.FaceInfo faceInfo,
		Color color,
		Texture uvs,
		FaceLight light,
		boolean renderBothSides
	) {
		quad(buffer, matrix, faceInfo.face, faceInfo.normal, color, uvs, light, renderBothSides);
	}

	static void quad(
		VertexConsumer buffer, PoseStack matrix,
		Face face, Vec faceNormal,
		Color color,
		Texture uvs,
		FaceLight light,
		boolean doubleSided
	) {
		quad(
			buffer, matrix, doubleSided,
			face, color, uvs, OverlayTexture.NO_OVERLAY, light, faceNormal
		);
	}

	static void quad(
		VertexConsumer buffer, PoseStack matrix, boolean doubleSided,
		Face face, Color color, Texture texture, int overlay, FaceLight light, Vec normal
	) {
		quad(
			buffer, matrix, doubleSided,
			face.v0, color, texture.u0, texture.v0, overlay, light.v0, normal,
			face.v1, color, texture.u1, texture.v1, overlay, light.v1, normal,
			face.v2, color, texture.u2, texture.v2, overlay, light.v2, normal,
			face.v3, color, texture.u3, texture.v3, overlay, light.v3, normal
		);
	}

	static void quad(
		VertexConsumer buffer, PoseStack matrix, boolean doubleSided,
		Vec vec0, Color color0, float u0, float v0, int overlay0, int light0, Vec normal0,
		Vec vec1, Color color1, float u1, float v1, int overlay1, int light1, Vec normal1,
		Vec vec2, Color color2, float u2, float v2, int overlay2, int light2, Vec normal2,
		Vec vec3, Color color3, float u3, float v3, int overlay3, int light3, Vec normal3
	) {
		quad(
			buffer, matrix, doubleSided,
			vec0.x, vec0.y, vec0.z, color0.red, color0.green, color0.blue, color0.alpha, u0, v0, overlay0, light0, normal0.x, normal0.y, normal0.z,
			vec1.x, vec1.y, vec1.z, color1.red, color1.green, color1.blue, color1.alpha, u1, v1, overlay1, light1, normal1.x, normal1.y, normal1.z,
			vec2.x, vec2.y, vec2.z, color2.red, color2.green, color2.blue, color2.alpha, u2, v2, overlay2, light2, normal2.x, normal2.y, normal2.z,
			vec3.x, vec3.y, vec3.z, color3.red, color3.green, color3.blue, color3.alpha, u3, v3, overlay3, light3, normal3.x, normal3.y, normal3.z
		);
	}

	static void quad(
		VertexConsumer buffer, PoseStack matrix, boolean doubleSided,
		float v0x, float v0y, float v0z, float red0, float green0, float blue0, float alpha0, float u0, float v0, int overlay0, int light0, float n0x, float n0y, float n0z,
		float v1x, float v1y, float v1z, float red1, float green1, float blue1, float alpha1, float u1, float v1, int overlay1, int light1, float n1x, float n1y, float n1z,
		float v2x, float v2y, float v2z, float red2, float green2, float blue2, float alpha2, float u2, float v2, int overlay2, int light2, float n2x, float n2y, float n2z,
		float v3x, float v3y, float v3z, float red3, float green3, float blue3, float alpha3, float u3, float v3, int overlay3, int light3, float n3x, float n3y, float n3z
	) {
		vertex(buffer, matrix, v0x, v0y, v0z, red0, green0, blue0, alpha0, u0, v0, overlay0, light0, n0x, n0y, n0z);
		vertex(buffer, matrix, v1x, v1y, v1z, red1, green1, blue1, alpha1, u1, v1, overlay1, light1, n1x, n1y, n1z);
		vertex(buffer, matrix, v2x, v2y, v2z, red2, green2, blue2, alpha2, u2, v2, overlay2, light2, n2x, n2y, n2z);
		vertex(buffer, matrix, v3x, v3y, v3z, red3, green3, blue3, alpha3, u3, v3, overlay3, light3, n3x, n3y, n3z);
		if (doubleSided) {
			vertex(buffer, matrix, v0x, v0y, v0z, red0, green0, blue0, alpha0, u0, v0, overlay0, light0, n0x, n0y, n0z);
			vertex(buffer, matrix, v3x, v3y, v3z, red3, green3, blue3, alpha3, u3, v3, overlay3, light3, n3x, n3y, n3z);
			vertex(buffer, matrix, v2x, v2y, v2z, red2, green2, blue2, alpha2, u2, v2, overlay2, light2, n2x, n2y, n2z);
			vertex(buffer, matrix, v1x, v1y, v1z, red1, green1, blue1, alpha1, u1, v1, overlay1, light1, n1x, n1y, n1z);
		}
	}

}

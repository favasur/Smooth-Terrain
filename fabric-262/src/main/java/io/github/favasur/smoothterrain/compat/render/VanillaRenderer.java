package io.github.favasur.smoothterrain.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.favasur.smoothterrain.client.render.struct.Color;
import io.github.favasur.smoothterrain.client.render.struct.FaceLight;
import io.github.favasur.smoothterrain.client.render.struct.Texture;
import io.github.favasur.smoothterrain.util.Face;
import io.github.favasur.smoothterrain.util.Vec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static io.github.favasur.smoothterrain.client.RenderHelper.vertex;

/**
 * 26.2 adaptation of the canonical SmoothTerrain chunk renderer. The 26.2 chunk pipeline differs
 * from 1.21.1: quads come from {@code BlockStateModel.collectParts} +
 * {@code BlockStateModelPart.getQuads(Direction)} (the {@code BakedModel.getQuads} API is gone),
 * each quad carries its own {@link ChunkSectionLayer} in its material info (no per-state render
 * type lookup), and vertices are written through the 26.2 {@link VertexConsumer} 12-arg
 * {@code addVertex}. The {@link SectionCompilerMixin} calls {@link #renderChunk} at the point the
 * vanilla per-block loop finishes, before the section's buffers are built into mesh data.
 */
public final class VanillaRenderer {

	public static void renderChunk(BlockPos sectionOrigin, PoseStack matrix, BlockAndTintGetter world,
	                               Function<ChunkSectionLayer, VertexConsumer> bufferFactory) {
		if (MeshRenderer.shouldSkipChunkMeshRendering())
			return;

		BlockColors blockColors = Minecraft.getInstance().getBlockColors();
		var blockModelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
		ModelBlockRenderer blockRenderer = new ModelBlockRenderer(false, false, blockColors);
		RandomSource random = RandomSource.create();
		List<BlockStateModelPart> parts = new ArrayList<>();

		matrix.pushPose();
		try {
			var objects = MeshRenderer.MutableObjects.INSTANCE.get();
			MeshRenderer.renderChunk(
				world, sectionOrigin,
				new MeshRenderer.ISmoothTerrainAreaRenderer() {
					@Override
					public void quad(BlockState state, BlockPos worldPos, MeshRenderer.FaceInfo faceInfo, boolean renderBothSides, Color colorOverride, LightCache lightCache, float shade) {
						var light = lightCache.get(sectionOrigin, faceInfo.face, faceInfo.normal, objects.light);
						var direction = faceInfo.approximateDirection;
						random.setSeed(state.getSeed(worldPos));
						parts.clear();
						blockModelSet.get(state).collectParts(random, parts);
						for (var part : parts) {
							var quads = part.getQuads(direction);
							if (quads.isEmpty())
								quads = part.getQuads(null);
							for (var quad : quads) {
								var buffer = bufferFactory.apply(quad.materialInfo().layer());
								var texture = Texture.forQuadRearranged(objects.texture, quad, direction);
								var color = colorOverride != null ? colorOverride : applyColorTo(world, blockColors, objects.color, quad, state, worldPos, shade);
								renderQuad(buffer, matrix, faceInfo, color, texture, light, renderBothSides);
							}
						}
					}

					@Override
					public void block(BlockState state, BlockPos worldPos, float relativeX, float relativeY, float relativeZ) {
						// Whole blocks the mesher keeps in place (short plants with fixPlantHeight)
						// go through the vanilla 26.2 block pipeline into the same section buffers.
						BlockQuadOutput output = (x, y, z, quad, instance) -> {
							var buffer = bufferFactory.apply(quad.materialInfo().layer());
							buffer.putBlockBakedQuad(x, y, z, quad, instance);
						};
						blockRenderer.tesselateBlock(
							output, relativeX, relativeY, relativeZ,
							world, worldPos, state, blockModelSet.get(state), state.getSeed(worldPos)
						);
					}
				}
			);
		} finally {
			matrix.popPose();
		}
	}

	static Color applyColorTo(
		BlockAndTintGetter world, BlockColors blockColors,
		Color color, BakedQuad quad,
		BlockState state, BlockPos pos,
		float shade
	) {
		if (!quad.materialInfo().isTinted()) {
			color.red = shade;
			color.green = shade;
			color.blue = shade;
			return color;
		}
		var tintSource = blockColors.getTintSource(state, quad.materialInfo().tintIndex());
		int packedColor = tintSource == null ? -1 : tintSource.colorInWorld(state, world, pos);
		color.red = (float) (packedColor >> 16 & 255) / 255.0F * shade;
		color.green = (float) (packedColor >> 8 & 255) / 255.0F * shade;
		color.blue = (float) (packedColor & 255) / 255.0F * shade;
		return color;
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

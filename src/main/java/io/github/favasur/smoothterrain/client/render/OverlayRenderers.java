package io.github.favasur.smoothterrain.client.render;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.client.RollingProfiler;
import io.github.favasur.smoothterrain.client.render.struct.Color;
import io.github.favasur.smoothterrain.collision.CollisionHandler;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.util.Area;
import io.github.favasur.smoothterrain.util.ModUtil;
import io.github.favasur.smoothterrain.util.Vec;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.github.favasur.smoothterrain.client.RenderHelper.*;
import static io.github.favasur.smoothterrain.client.render.MeshRenderer.FaceInfo;
import static io.github.favasur.smoothterrain.client.render.MeshRenderer.ISmoothTerrainAreaRenderer;
import static net.minecraft.core.BlockPos.MutableBlockPos;

/**
 * @author Cadiboo
 */
public final class OverlayRenderers {

	public static void register(Consumer<Consumer<PoseStack>> registerPerFrameHandler) {
		var meshProfiler = new RollingProfiler(600, "Render wireframe mesh");
		var debugOverlays = Lists.<Pair<String, DebugOverlay>>newArrayList(
			Pair.of("drawOutlineAroundNearbySmoothableBlocks", OverlayRenderers::drawOutlineAroundNearbySmoothableBlocks),
			Pair.of("drawOutlineAroundNearbySmoothableBlocks", OverlayRenderers::drawOutlineAroundNearbySmoothableBlocks),
			Pair.of("maybeRenderMeshAndRecordPerformance", (camera, matrix, lines) -> maybeRenderMeshAndRecordPerformance(camera, matrix, lines, meshProfiler)),
			Pair.of("drawNearbyMeshCollisions", OverlayRenderers::drawNearbyMeshCollisions),
			Pair.of("drawNearbyCollisions", OverlayRenderers::drawNearbyCollisions),
			Pair.of("drawNearbyDensities", OverlayRenderers::drawNearbyDensities)
		);
		registerPerFrameHandler.accept(matrix -> renderDebugOverlays(matrix, debugOverlays));
	}

	public static boolean renderSmoothTerrainBlockHighlight(
		PoseStack matrix, VertexConsumer buffer,
		double cameraX, double cameraY, double cameraZ,
		BlockAndTintGetter world, BlockPos lookingAtPos, BlockState state
	) {
		if (!SmoothTerrainConfig.Client.renderSelectionBox)
			return false;
		if (!SmoothTerrainConfig.Client.render)
			return false;
		var color = SmoothTerrainConfig.Client.selectionBoxColor;
		return MeshRenderer.renderSingleBlock(world, lookingAtPos, state, new ISmoothTerrainAreaRenderer() {
			@Override
			public void quad(BlockState state, BlockPos worldPos, FaceInfo faceInfo, boolean renderBothSides, io.github.favasur.smoothterrain.client.render.struct.Color colorOverride, LightCache lightCache, float shade) {
				drawFacePosColor(
					faceInfo.face,
					cameraX, cameraY, cameraZ,
					lookingAtPos, color,
					buffer, matrix
				);
			}

			@Override
			public void block(BlockState state, BlockPos worldPos, float relativeX, float relativeY, float relativeZ) {
				drawShape(
					matrix, buffer,
					state.getShape(world, worldPos),
					lookingAtPos, relativeX, relativeY, relativeZ,
					cameraX, cameraY, cameraZ,
					color
				);
			}
		});
	}

	static void renderDebugOverlays(PoseStack matrix, List<Pair<String, DebugOverlay>> overlays) {
		if (!SmoothTerrainConfig.Common.debugEnabled)
			return;

		var minecraft = Minecraft.getInstance();
		var camera = minecraft.gameRenderer.getMainCamera();

//		drawBlockDestructionProgressForDebug(minecraft, camera);
		var bufferSource = minecraft.renderBuffers().bufferSource();
		Supplier<VertexConsumer> linesSupplier = () -> bufferSource.getBuffer(RenderType.lines());
		overlays.forEach(overlay -> {
			var profiler = minecraft.getProfiler();
			profiler.push(overlay.getLeft());
			try {
				overlay.getRight().render(camera, matrix, linesSupplier);
			} finally {
				profiler.pop();
			}
		});

		// Hack to finish buffer because RenderWorldLastEvent seems to fire after vanilla normally finishes them
		bufferSource.endBatch(RenderType.lines());
	}

	interface DebugOverlay {
		void render(Camera camera, PoseStack matrix, Supplier<VertexConsumer> linesSupplier);
	}

	private static BlockPos getTargetedPosForDebugRendering(Entity viewer) {
		var targeted = viewer.pick(20.0D, 0.0F, false);
		// Where the player is looking at or their position of they're not looking at a block
		return targeted.getType() != HitResult.Type.BLOCK ? viewer.blockPosition() : ((BlockHitResult) targeted).getBlockPos();
	}

	/**
	 * Draws block destruction progress (cracking texture) near the viewer.
	 * Was used to debug issues with matrix transformations while implementing our custom destruction progress rendering.
	 */
	private static void drawBlockDestructionProgressForDebug(Minecraft minecraft, Camera camera) {
		var viewer = camera.getEntity();
		var targetedPos = getTargetedPosForDebugRendering(viewer);
		var start = targetedPos.offset(-2, -2, -2);
		var end = targetedPos.offset(2, 2, 2);
		var i = new int[] {0};
		BlockPos.betweenClosed(start, end)
			.forEach(pos -> minecraft.levelRenderer.destroyBlockProgress(100 + i[0]++, pos, 9));
	}

	private static void drawOutlineAroundNearbySmoothableBlocks(Camera camera, PoseStack matrix, Supplier<VertexConsumer> buffer) {
		if (!SmoothTerrainConfig.Client.debugOutlineSmoothables)
			return;
		var viewer = camera.getEntity();
		Predicate<BlockState> isSmoothable = SmoothTerrain.smoothableHandler::isSmoothable;
		var color = new Color(0, 1, 0, 0.4F);
		var start = viewer.blockPosition().offset(-5, -5, -5);
		var end = viewer.blockPosition().offset(5, 5, 5);
		BlockPos.betweenClosed(start, end).forEach(pos -> {
			if (isSmoothable.test(viewer.level().getBlockState(pos)))
				drawShape(matrix, buffer.get(), Shapes.block(), pos, camera.getPosition(), color);
		});
	}

	private static void drawNearbyDensities(Camera camera, PoseStack matrix, Supplier<VertexConsumer> buffer) {
		// Draw nearby block densities and computed corner signed distance fields
		// This was just for understanding how SurfaceNets works
		// It made me understand why feeding it the 'proper' corner info results in much smoother terrain
		// at the cost of 1-block formations disappearing
		if (!SmoothTerrainConfig.Client.debugVisualiseDensitiesGrid)
			return;
		Predicate<BlockState> isSmoothable = SmoothTerrain.smoothableHandler::isSmoothable;
		var distanceIndicator = Shapes.box(0, 0, 0, 1 / 8F, 1 / 8F, 1 / 8F);
		var densityColor = new Color(0F, 0F, 1F, 0.5F);
		var viewer = camera.getEntity();
		try (var area = new Area(viewer.level(), getTargetedPosForDebugRendering(viewer).offset(-2, -2, -2), new BlockPos(4, 4, 4), SmoothTerrainConfig.Server.mesher)) {
			var states = area.getAndCacheBlocks();
			var densities = new float[area.numBlocks()];
			for (int i = 0; i < densities.length; ++i)
				densities[i] = ModUtil.getBlockDensity(isSmoothable, states[i]);

			int minZ = area.start.getZ();
			int minY = area.start.getY();
			int minX = area.start.getX();
			int width = area.size.getX();
			int height = area.size.getY();
			int maxZ = minZ + area.size.getZ();
			int maxY = minY + height;
			int maxX = minX + width;
			int zyxIndex = 0;
			var pos = new MutableBlockPos();
			for (int z = minZ; z < maxZ; ++z) {
				for (int y = minY; y < maxY; ++y) {
					for (int x = minX; x < maxX; ++x, ++zyxIndex) {
						pos.set(x, y, z);
						var density = densities[zyxIndex];
						var densityScale = 0.5F + density / 2F; // from [-1, 1] -> [0, 1]
						if (densityScale > 0.01) {
							var box = Shapes.box(0.5 - densityScale / 2, 0.5 - densityScale / 2, 0.5 - densityScale / 2, 0.5 + densityScale / 2, 0.5 + densityScale / 2, 0.5 + densityScale / 2);
							drawShape(matrix, buffer.get(), box, pos, camera.getPosition(), densityColor);
						}
						if (x <= minX || y <= minY || z <= minZ)
							continue;

						float combinedDensity = 0; // AKA signed distance field
						int idx = zyxIndex;
						for (int cornerZ = 0; cornerZ < 2; ++cornerZ, idx -= width * (height - 2))
							for (int cornerY = 0; cornerY < 2; ++cornerY, idx -= width - 2)
								for (byte cornerX = 0; cornerX < 2; ++cornerX, --idx) {
									combinedDensity += densities[idx];
								}
						float combinedDensityScale = 0.5F + combinedDensity / 16F; // from [-8, 8] -> [0, 1]
						drawShape(matrix, buffer.get(), distanceIndicator, pos, camera.getPosition(), new Color(combinedDensityScale, 1 - combinedDensityScale, 0F, 0.4F));
					}
				}
			}
		}
	}

	private static void drawNearbyCollisions(Camera camera, PoseStack matrix, Supplier<VertexConsumer> buffer) {
		// Draw nearby collisions in green and player intersecting collisions in red
		if (!SmoothTerrainConfig.Client.debugRenderCollisions)
			return;
		var collisionsRenderRadius = 10;
		var intersectingColor = new Color(1, 0, 0, 0.4F);
		var deviatingColor = new Color(0, 1, 0, 0.4F);
		var viewer = camera.getEntity();
		var viewerShape = Shapes.create(viewer.getBoundingBox());
		viewer.level().getBlockCollisions(viewer, viewer.getBoundingBox().inflate(collisionsRenderRadius)).forEach(voxelShape -> {
			boolean intersects = Shapes.joinIsNotEmpty(voxelShape, viewerShape, BooleanOp.AND);
			drawShape(matrix, buffer.get(), voxelShape, BlockPos.ZERO, camera.getPosition(), intersects ? intersectingColor : deviatingColor);
		});
	}

	private static void drawNearbyMeshCollisions(Camera camera, PoseStack matrix, Supplier<VertexConsumer> buffer) {
		// Draw SmoothTerrain' collisions in green (or yellow if debugRenderCollisions is enabled)
		if (!SmoothTerrainConfig.Client.debugRenderMeshCollisions)
			return;
		var collisionsRenderRadius = 10;
		var color = new Color(SmoothTerrainConfig.Client.debugRenderCollisions ? 1 : 0, 1, 0, 0.4F);
		var viewer = camera.getEntity();
		var start = viewer.blockPosition().offset(-collisionsRenderRadius, -collisionsRenderRadius, -collisionsRenderRadius);
		CollisionHandler.forEachCollisionShapeRelativeToStart(viewer.level(), new MutableBlockPos(),
			start.getX(), start.getX() + collisionsRenderRadius * 2,
			start.getY(), start.getY() + collisionsRenderRadius * 2,
			start.getZ(), start.getZ() + collisionsRenderRadius * 2,
			shape -> {
				drawShape(matrix, buffer.get(), shape, start, camera.getPosition(), color);
				return true;
			}
		);
	}

	private static void maybeRenderMeshAndRecordPerformance(Camera camera, PoseStack matrix, Supplier<VertexConsumer> linesSupplier, RollingProfiler profiler) {
		// Measure the performance of meshing nearby blocks (and maybe render the result)
		if (!SmoothTerrainConfig.Client.debugRecordMeshPerformance && !SmoothTerrainConfig.Client.debugOutlineNearbyMesh)
			return;
		var buffer = linesSupplier.get();
		var startNanos = System.nanoTime();
		drawNearbyMesh(camera.getEntity(), camera.getPosition(), matrix, buffer);
		if (SmoothTerrainConfig.Client.debugRecordMeshPerformance && profiler.recordElapsedNanos(startNanos))
			LogManager.getLogger("Calc" + (SmoothTerrainConfig.Client.debugOutlineNearbyMesh ? " & outline" : "") + " nearby mesh").debug("Average {}ms over the past {} frames", profiler.average() / 1000_000F, profiler.size());
	}

	private static void drawNearbyMesh(Entity viewer, Vec3 camera, PoseStack matrix, VertexConsumer buffer) {
		var faceColor = new Color(0F, 1F, 1F, 0.4F);
		var normalColor = new Color(0F, 0F, 1F, 0.2F);
		var averageNormalColor = new Color(1F, 0F, 0F, 0.4F);
		var normalDirectionColor = new Color(0F, 1F, 0F, 1F);
		var textureColor = new Color(1F, 1F, 1F, 1F);
		var lightColor = new Color(1F, 1F, 0F, 1F);

		var meshSize = new BlockPos(16, 16, 16);
		var meshStart = viewer.blockPosition().offset(-meshSize.getX() / 2, -meshSize.getY() / 2 + 2, -meshSize.getZ() / 2);

		matrix.pushPose();
		try {
			var world = viewer.level();
			var mutable = new Vec();
			MeshRenderer.renderArea(
				world, meshStart, meshSize,
				SmoothTerrain.smoothableHandler::isSmoothable,
				new ISmoothTerrainAreaRenderer() {
					@Override
					public void quad(BlockState state, BlockPos pos, FaceInfo faceInfo, boolean renderBothSides, io.github.favasur.smoothterrain.client.render.struct.Color colorOverride, LightCache lightCache, float shade) {
						if (!SmoothTerrainConfig.Client.debugOutlineNearbyMesh)
							return;
						var face = faceInfo.face;
						drawFacePosColor(face, camera, meshStart, faceColor, buffer, matrix);

						// Draw face normal vec + resulting direction
						final float dirMul = 0.2F;
						drawLinePosColorFromAdd(meshStart, faceInfo.centre, mutable.set(faceInfo.normal).multiply(dirMul), averageNormalColor, buffer, matrix, camera);
						drawLinePosColorFromAdd(meshStart, faceInfo.centre, mutable.set(faceInfo.approximateDirection.getStepX(), faceInfo.approximateDirection.getStepY(), faceInfo.approximateDirection.getStepZ()).multiply(dirMul), normalDirectionColor, buffer, matrix, camera);

						// Draw each vertex normal
						drawLinePosColorFromAdd(meshStart, face.v0, mutable.set(faceInfo.vertexNormals.v0).multiply(dirMul), normalColor, buffer, matrix, camera);
						drawLinePosColorFromAdd(meshStart, face.v1, mutable.set(faceInfo.vertexNormals.v1).multiply(dirMul), normalColor, buffer, matrix, camera);
						drawLinePosColorFromAdd(meshStart, face.v2, mutable.set(faceInfo.vertexNormals.v2).multiply(dirMul), normalColor, buffer, matrix, camera);
						drawLinePosColorFromAdd(meshStart, face.v3, mutable.set(faceInfo.vertexNormals.v3).multiply(dirMul), normalColor, buffer, matrix, camera);

						// Draw texture pos (will have been set by caller)
						mutable.set(0.5F, 0.5F, 0.5F);
						drawLinePosColorFromTo(meshStart, faceInfo.centre, pos, mutable, textureColor, buffer, matrix, camera);

		//				// Draw light pos
		//				mutable.set(0, 0, 0);
		//				var faceRelativeToWorldPos = faceInfo.faceRelativeToWorldPos;
		//				if (light.get(faceRelativeToWorldPos, face.v0, faceNormal) == 0)
		//					drawLinePosColorFromTo(area.start, face.v0, light.lightWorldPos(area.start, face.v0, faceNormal), mutable, lightColor, buffer, matrix, camera);
		//				if (light.get(faceRelativeToWorldPos, face.v1, faceNormal) == 0)
		//					drawLinePosColorFromTo(area.start, face.v1, light.lightWorldPos(area.start, face.v1, faceNormal), mutable, lightColor, buffer, matrix, camera);
		//				if (light.get(faceRelativeToWorldPos, face.v2, faceNormal) == 0)
		//					drawLinePosColorFromTo(area.start, face.v2, light.lightWorldPos(area.start, face.v2, faceNormal), mutable, lightColor, buffer, matrix, camera);
		//				if (light.get(faceRelativeToWorldPos, face.v3, faceNormal) == 0)
		//					drawLinePosColorFromTo(area.start, face.v3, light.lightWorldPos(area.start, face.v3, faceNormal), mutable, lightColor, buffer, matrix, camera);

					}

					@Override
					public void block(BlockState state, BlockPos worldPos, float relativeX, float relativeY, float relativeZ) {
						if (!SmoothTerrainConfig.Client.debugOutlineNearbyMesh)
							return;
						drawShape(
							matrix, buffer,
							state.getShape(world, worldPos),
							meshStart, relativeX, relativeY, relativeZ,
							camera,
							faceColor
						);
					}
				}
			);
		} finally {
			matrix.popPose();
		}
	}

}

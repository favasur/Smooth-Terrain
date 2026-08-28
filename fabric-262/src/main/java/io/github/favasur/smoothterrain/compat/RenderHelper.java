package io.github.favasur.smoothterrain.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.favasur.smoothterrain.client.render.struct.Color;
import io.github.favasur.smoothterrain.util.Face;
import io.github.favasur.smoothterrain.util.Vec;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * 26.2 adaptation of the canonical rendering helpers. {@link #reloadAllChunks} uses the 26.2
 * {@code LevelRenderer#invalidateCompiledGeometry} recompile path (the 1.21.1 {@code allChanged}
 * no longer exists) and ARGB packing is inlined (26.2 removed {@code FastColor.ARGB32}).
 */
public class RenderHelper {

	private static final Logger LOG = LogManager.getLogger();

	public static void reloadAllChunks(String because, Object... becauseArgs) {
		LOG.debug(() -> "Re-rendering chunks because " + because.formatted(becauseArgs));
		// Config/mesher/smoothable changes invalidate every cached mesh; the next render/collision
		// query rebuilds them from the current world state.
		io.github.favasur.smoothterrain.mesh.MeshCache.clear();
		var minecraft = Minecraft.getInstance();
		minecraft.execute(() -> {
			if (minecraft.level == null)
				return;
			var level = minecraft.level;
			var options = minecraft.options;
			// 26.2's GameRenderer keeps its camera private; a fresh Camera is fine here because the
			// render loop repositions the view area every frame anyway (the position is only used
			// for the initial section reposition inside invalidateCompiledGeometry).
			var camera = new Camera();
			var blockColors = minecraft.getBlockColors();
			minecraft.levelRenderer.invalidateCompiledGeometry(level, options, camera, blockColors);
		});
	}

	public static void drawLinePosColorFromAdd(BlockPos offset, Vec start, Vec add, Color color, VertexConsumer buffer, PoseStack matrix, Vec3 camera) {
		var startX = (float) (offset.getX() - camera.x + start.x);
		var startY = (float) (offset.getY() - camera.y + start.y);
		var startZ = (float) (offset.getZ() - camera.z + start.z);
		line(
			buffer, matrix, color,
			startX, startY, startZ,
			startX + add.x, startY + add.y, startZ + add.z
		);
	}

	public static void drawLinePosColorFromTo(BlockPos startOffset, Vec start, BlockPos endOffset, Vec end, Color color, VertexConsumer buffer, PoseStack matrix, Vec3 camera) {
		line(
			buffer, matrix, color,
			(float) (startOffset.getX() + start.x - camera.x), (float) (startOffset.getY() + start.y - camera.y), (float) (startOffset.getZ() + start.z - camera.z),
			(float) (endOffset.getX() + end.x - camera.x), (float) (endOffset.getY() + end.y - camera.y), (float) (endOffset.getZ() + end.z - camera.z)
		);
	}

	public static void drawFacePosColor(
		Face face, Vec3 camera,
		BlockPos pos, Color color,
		VertexConsumer buffer, PoseStack matrix
	) {
		drawFacePosColor(
			face, camera.x, camera.y, camera.z,
			pos, color, buffer, matrix
		);
	}

	public static void drawFacePosColor(
		Face face, double cameraX, double cameraY, double cameraZ,
		BlockPos pos, Color color,
		VertexConsumer buffer, PoseStack matrix
	) {
		var v0 = face.v0;
		var v1 = face.v1;
		var v2 = face.v2;
		var v3 = face.v3;
		var x = pos.getX() - cameraX;
		var y = pos.getY() - cameraY;
		var z = pos.getZ() - cameraZ;

		var v0x = (float) (x + v0.x);
		var v1x = (float) (x + v1.x);
		var v2x = (float) (x + v2.x);
		var v3x = (float) (x + v3.x);
		var v0y = (float) (y + v0.y);
		var v1y = (float) (y + v1.y);
		var v2y = (float) (y + v2.y);
		var v3y = (float) (y + v3.y);
		var v0z = (float) (z + v0.z);
		var v1z = (float) (z + v1.z);
		var v2z = (float) (z + v2.z);
		var v3z = (float) (z + v3.z);
		line(buffer, matrix, color, v0x, v0y, v0z, v1x, v1y, v1z);
		line(buffer, matrix, color, v1x, v1y, v1z, v2x, v2y, v2z);
		line(buffer, matrix, color, v2x, v2y, v2z, v3x, v3y, v3z);
		line(buffer, matrix, color, v3x, v3y, v3z, v0x, v0y, v0z);
	}

	public static void drawShape(
		PoseStack stack, VertexConsumer buffer,
		VoxelShape shape, BlockPos pos,
		Vec3 camera, Color color
	) {
		drawShape(
			stack, buffer, shape,
			pos, 0, 0, 0,
			camera, color
		);
	}

	public static void drawShape(
		PoseStack stack, VertexConsumer buffer,
		VoxelShape shape, BlockPos pos,
		float xOffset, float yOffset, float zOffset,
		Vec3 camera, Color color
	) {
		drawShape(
			stack, buffer, shape,
			pos,
			xOffset, yOffset, zOffset,
			camera.x, camera.y, camera.z,
			color
		);
	}

	public static void drawShape(
		PoseStack stack, VertexConsumer buffer,
		VoxelShape shape, BlockPos pos,
		float xOffset, float yOffset, float zOffset,
		double cameraX, double cameraY, double cameraZ,
		Color color
	) {
		var x = pos.getX() + xOffset - cameraX;
		var y = pos.getY() + yOffset - cameraY;
		var z = pos.getZ() + zOffset - cameraZ;
		shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> line(
			buffer, stack, color,
			(float) (x + x0), (float) (y + y0), (float) (z + z0),
			(float) (x + x1), (float) (y + y1), (float) (z + z1)
		));
	}

	/** Inlined {@code FastColor.ARGB32.color} - 26.2 removed the FastColor helper. */
	public static int argb(float alpha, float red, float green, float blue) {
		return ((int) (alpha * 255) << 24) | ((int) (red * 255) << 16) | ((int) (green * 255) << 8) | (int) (blue * 255);
	}

	/**
	 * This is both a convenience method and an optimisation.
	 * Calling 'buffer.vertex(pose, x, y, z)' or 'buffer.normal(normal, x, y, z)' allocate a Vector4f and a Vector3f respectively.
	 * To avoid allocating many short-lived vectors we do the transform ourselves instead.
	 */
	public static void line(
		VertexConsumer buffer, PoseStack matrix, Color color,
		float x0, float y0, float z0,
		float x1, float y1, float z1
	) {
		var currentTransform = matrix.last();
		var pose = currentTransform.pose();
		var normal = currentTransform.normal();
		var transformedX0 = getTransformX(pose, x0, y0, z0, 1);
		var transformedY0 = getTransformY(pose, x0, y0, z0, 1);
		var transformedZ0 = getTransformZ(pose, x0, y0, z0, 1);
		var transformedX1 = getTransformX(pose, x1, y1, z1, 1);
		var transformedY1 = getTransformY(pose, x1, y1, z1, 1);
		var transformedZ1 = getTransformZ(pose, x1, y1, z1, 1);

		var normalX = x1 - x0;
		var normalY = y1 - y0;
		var normalZ = z1 - z0;
		var length = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
		normalX /= length;
		normalY /= length;
		normalZ /= length;

		var transformedNormalX = getTransformX(normal, normalX, normalY, normalZ);
		var transformedNormalY = getTransformY(normal, normalX, normalY, normalZ);
		var transformedNormalZ = getTransformZ(normal, normalX, normalY, normalZ);

		buffer
			.addVertex(transformedX0, transformedY0, transformedZ0)
			.setColor(argb(color.alpha, color.red, color.green, color.blue))
			.setNormal(transformedNormalX, transformedNormalY, transformedNormalZ);
		buffer
			.addVertex(transformedX1, transformedY1, transformedZ1)
			.setColor(argb(color.alpha, color.red, color.green, color.blue))
			.setNormal(transformedNormalX, transformedNormalY, transformedNormalZ);
	}

	/**
	 * This is both a convenience method and an optimisation.
	 * Calling 'buffer.vertex(pose, x, y, z)' or 'buffer.normal(normal, x, y, z)' allocate a Vector4f and a Vector3f respectively.
	 * To avoid allocating many short-lived vectors we do the transform ourselves instead.
	 */
	public static void vertex(
		VertexConsumer buffer, PoseStack matrix,
		float x, float y, float z,
		float red, float green, float blue, float alpha,
		float texU, float texV,
		int overlayUV, int lightmapUV,
		float normalX, float normalY, float normalZ
	) {
		var currentTransform = matrix.last();
		var pose = currentTransform.pose();
		var normal = currentTransform.normal();

		var transformedX = getTransformX(pose, x, y, z, 1);
		var transformedY = getTransformY(pose, x, y, z, 1);
		var transformedZ = getTransformZ(pose, x, y, z, 1);

		var transformedNormalX = getTransformX(normal, normalX, normalY, normalZ);
		var transformedNormalY = getTransformY(normal, normalX, normalY, normalZ);
		var transformedNormalZ = getTransformZ(normal, normalX, normalY, normalZ);

		buffer.addVertex(
			transformedX, transformedY, transformedZ,
			argb(alpha, red, green, blue),
			texU, texV,
			overlayUV, lightmapUV,
			transformedNormalX, transformedNormalY, transformedNormalZ
		);
	}

	/** See {@link org.joml.Vector3f#mul(Matrix3f)} */
	public static float getTransformX(Matrix3f matrix, float x, float y, float z) {
		return matrix.m00 * x + matrix.m01 * y + matrix.m02 * z;
	}

	/** See {@link org.joml.Vector3f#mul(Matrix3f)} */
	public static float getTransformY(Matrix3f matrix, float x, float y, float z) {
		return matrix.m10 * x + matrix.m11 * y + matrix.m12 * z;
	}

	/** See {@link org.joml.Vector3f#mul(Matrix3f)} */
	public static float getTransformZ(Matrix3f matrix, float x, float y, float z) {
		return matrix.m20 * x + matrix.m21 * y + matrix.m22 * z;
	}

	/** See {@link org.joml.Vector4f#mul(Matrix4fc)} */
	public static float getTransformX(Matrix4f matrix, float x, float y, float z, float w) {
		return matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30() * w;
	}

	/** See {@link org.joml.Vector4f#mul(Matrix4fc)} */
	public static float getTransformY(Matrix4f matrix, float x, float y, float z, float w) {
		return matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31() * w;
	}

	/** See {@link org.joml.Vector4f#mul(Matrix4fc)} */
	public static float getTransformZ(Matrix4f matrix, float x, float y, float z, float w) {
		return matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32() * w;
	}

	/** See {@link org.joml.Vector4f#mul(Matrix4fc)} */
	public static float getTransformW(Matrix4f matrix, float x, float y, float z, float w) {
		return matrix.m30() * x + matrix.m31() * y + matrix.m32() * z + matrix.m33() * w;
	}

}

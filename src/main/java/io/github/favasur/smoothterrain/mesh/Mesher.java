package io.github.favasur.smoothterrain.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.favasur.smoothterrain.collision.ShapeConsumer;
import io.github.favasur.smoothterrain.util.Area;
import io.github.favasur.smoothterrain.util.Face;
import io.github.favasur.smoothterrain.util.ModUtil;
import io.github.favasur.smoothterrain.util.PerformanceCriticalAllocation;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

import static net.minecraft.core.BlockPos.MutableBlockPos;

public interface Mesher {

	@PerformanceCriticalAllocation
	ThreadLocal<MutableBlockPos> POS_INSTANCE = ThreadLocal.withInitial(MutableBlockPos::new);
	@PerformanceCriticalAllocation
	ThreadLocal<Face> FACE_INSTANCE = ThreadLocal.withInitial(Face::new);

	@PerformanceCriticalAllocation
	ThreadLocal<MutableBlockPos> GEOMETRY_PROBE_POS = ThreadLocal.withInitial(MutableBlockPos::new);

	default void generateGeometry(Area area, Predicate<BlockState> isSmoothable, FaceAction action) {
		try {
			generateGeometryInternal(area, isSmoothable, (pos, face) -> {
				if (isDuplicateOfSolidBlockFace(area, isSmoothable, face))
					// The non-smoothable solid block's own vanilla faces already render this surface
					return true;
				return action.apply(pos, face);
			});
		} catch (Throwable t) {
			Util.pauseInIde(t);
			throw t;
		}
	}

	/**
	 * Solid (non-smoothable) blocks are treated as fully inside the density field (see
	 * {@link io.github.favasur.smoothterrain.util.ModUtil#getBlockDensity}), which makes smooth
	 * surfaces clamp to their faces. The mesher then also emits the solid block's own boundary
	 * faces (the faces between a solid block and air), which would exactly overlap the block's
	 * vanilla faces and z-fight with them. This skips such faces: a real sloped smooth surface
	 * never lies flat on a cell boundary, so only true cell-boundary faces are ever affected.
	 */
	static boolean isDuplicateOfSolidBlockFace(Area area, Predicate<BlockState> isSmoothable, Face face) {
		float minX = Math.min(Math.min(face.v0.x, face.v1.x), Math.min(face.v2.x, face.v3.x));
		float maxX = Math.max(Math.max(face.v0.x, face.v1.x), Math.max(face.v2.x, face.v3.x));
		float minY = Math.min(Math.min(face.v0.y, face.v1.y), Math.min(face.v2.y, face.v3.y));
		float maxY = Math.max(Math.max(face.v0.y, face.v1.y), Math.max(face.v2.y, face.v3.y));
		float minZ = Math.min(Math.min(face.v0.z, face.v1.z), Math.min(face.v2.z, face.v3.z));
		float maxZ = Math.max(Math.max(face.v0.z, face.v1.z), Math.max(face.v2.z, face.v3.z));
		float sx = maxX - minX;
		float sy = maxY - minY;
		float sz = maxZ - minZ;
		float cx = (minX + maxX) * 0.5F;
		float cy = (minY + maxY) * 0.5F;
		float cz = (minZ + maxZ) * 0.5F;
		if (sx <= 0.05F && sx <= sy && sx <= sz)
			return solidOnEitherSide(area, isSmoothable, cx - 0.01F, cy, cz) || solidOnEitherSide(area, isSmoothable, cx + 0.01F, cy, cz);
		if (sy <= 0.05F && sy <= sx && sy <= sz)
			return solidOnEitherSide(area, isSmoothable, cx, cy - 0.01F, cz) || solidOnEitherSide(area, isSmoothable, cx, cy + 0.01F, cz);
		if (sz <= 0.05F && sz <= sx && sz <= sy)
			return solidOnEitherSide(area, isSmoothable, cx, cy, cz - 0.01F) || solidOnEitherSide(area, isSmoothable, cx, cy, cz + 0.01F);
		return false;
	}

	static boolean solidOnEitherSide(Area area, Predicate<BlockState> isSmoothable, float x, float y, float z) {
		BlockState state = area.getBlockStateFaultTolerant(GEOMETRY_PROBE_POS.get().set(x, y, z));
		return !isSmoothable.test(state) && ModUtil.platform.isSolidRender(state);
	}

	default void generateCollisions(Area area, Predicate<BlockState> isSmoothable, ShapeConsumer action) {
		try {
			generateCollisionsInternal(area, isSmoothable, action);
		} catch (Throwable t) {
			Util.pauseInIde(t);
			throw t;
		}
	}

	void generateGeometryInternal(Area area, Predicate<BlockState> isSmoothable, FaceAction action);

	void generateCollisionsInternal(Area area, Predicate<BlockState> isSmoothable, ShapeConsumer action);

	Vec3i getPositiveAreaExtension();

	Vec3i getNegativeAreaExtension();

	/**
	 * A stable identity for this mesher configuration, used as part of the mesh cache key.
	 * Distinct configurations must return distinct values (e.g. the 2x smoothness variants).
	 */
	default String cacheId() {
		return getClass().getName();
	}

	interface FaceAction {

		/**
		 * @param relativePos The position of the face, positioned relatively to the start of the area
		 * @param face        The face, positioned relatively to the start of the area
		 * @return false if no more faces need to be generated
		 */
		boolean apply(MutableBlockPos relativePos, Face face);

	}

	/**
	 * The vertices in meshes are generated relative to {@link Area#start}.
	 * {@link Area#start} is not necessarily the place where the final mesh should be rendered.
	 * The difference between the start of the area and the position we are generating for
	 * This exists because:
	 * To render a 16x16x16 area you need the data of a 18x18x18 area (+1 voxel on each axis)
	 * So the area is going to start at chunkPos - 1 (and extend 18 blocks)
	 * And the vertices are going to be relative to the start of the area
	 * We need to add an offset to the vertices because we want them to be relative to the start of the chunk, not the area
	 */
	static void translateToMeshStart(PoseStack matrix, BlockPos areaStart, BlockPos renderStartPos) {
		matrix.translate(
			getMeshOffset(areaStart.getX(), renderStartPos.getX()),
			getMeshOffset(areaStart.getY(), renderStartPos.getY()),
			getMeshOffset(areaStart.getZ(), renderStartPos.getZ())
		);
	}

	static int getMeshOffset(int areaStart, int desiredStart) {
		return validateMeshOffset(areaStart - desiredStart);
	}

	/* private */
	static int validateMeshOffset(int meshOffset) {
		assert meshOffset <= 0 : "Meshers won't require a smaller area than they are generating a mesh for";
		assert meshOffset > -3 : "Meshers won't require more than 2 extra blocks on each axis";
		return meshOffset;
	}


}

package io.github.favasur.smoothterrain.client.render;

import io.github.favasur.smoothterrain.client.render.struct.FaceLight;
import io.github.favasur.smoothterrain.util.Face;
import io.github.favasur.smoothterrain.util.ModUtil;
import io.github.favasur.smoothterrain.util.ThreadLocalArrayCache;
import io.github.favasur.smoothterrain.util.Vec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

import static io.github.favasur.smoothterrain.client.render.MeshRenderer.FaceInfo;

/**
 * 26.2 adaptation of the canonical light cache. 1.21.1's {@code LevelRenderer.getLightColor} and
 * {@code LightTexture.pack} are gone; 26.2 computes combined light through
 * {@link BlockModelLighter#getLightCoords} and packs with {@link LightCoordsUtil} (block in bits
 * 4-7, sky in bits 20-23).
 */
public final class LightCache implements AutoCloseable {

	public static final int MAX_BRIGHTNESS = LightCoordsUtil.FULL_BRIGHT;
	private static final ThreadLocalArrayCache<int[]> CACHE = new ThreadLocalArrayCache<>(int[]::new, array -> array.length, LightCache::resetIntArray);
	private static final ThreadLocal<BlockPos.MutableBlockPos> POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

	public final BlockPos start;
	public final BlockPos size;
	private final ClientLevel world;
	private int[] array;

	public LightCache(ClientLevel world, BlockPos meshStart, BlockPos meshSize) {
		this.world = world;
		this.start = meshStart.offset(-1, -1, -1).immutable();
		this.size = meshSize.offset(2, 2, 2).immutable();
	}

	private static void resetIntArray(int[] array, int length) {
		Arrays.fill(array, 0, length, -1);
	}

	/**
	 * Gets the position in world space to use to get light values for this vertex
	 */
	public BlockPos.MutableBlockPos lightWorldPos(BlockPos relativeTo, Vec vec, Vec normal) {
		float vx = -0.5F + vec.x + Mth.clamp(normal.x * 4, -1, 1);
		float vy = -0.5F + vec.y + Mth.clamp(normal.y * 4, -1, 1);
		float vz = -0.5F + vec.z + Mth.clamp(normal.z * 4, -1, 1);

		int x = Math.round(vx);
		int y = Math.round(vy);
		int z = Math.round(vz);
		return POS.get().set(relativeTo).move(x, y, z);
	}

	public FaceLight get(BlockPos relativeTo, FaceInfo faceInfo, FaceLight faceLight) {
		return get(relativeTo, faceInfo.face, faceInfo.normal, faceLight);
	}

	public FaceLight get(BlockPos relativeTo, Face face, Vec faceNormal, FaceLight faceLight) {
		faceLight.v0 = get(relativeTo, face.v0, faceNormal);
		faceLight.v1 = get(relativeTo, face.v1, faceNormal);
		faceLight.v2 = get(relativeTo, face.v2, faceNormal);
		faceLight.v3 = get(relativeTo, face.v3, faceNormal);
		return faceLight;
	}

	public int get(BlockPos relativeTo, Vec vec, Vec faceNormal) {
		BlockPos.MutableBlockPos lightWorldPos = lightWorldPos(relativeTo, vec, faceNormal);
		int light = get(lightWorldPos);
		if (light == 0)
			light = get(lightWorldPos.move(0, -1, 0));
		if (light == 0)
			light = get(lightWorldPos.move(0, 2, 0));
		if (light == 0)
			light = get(lightWorldPos.move(-1, -1, 0));
		if (light == 0)
			light = get(lightWorldPos.move(2, 0, 0));
		if (light == 0)
			light = get(lightWorldPos.move(-1, 0, -1));
		if (light == 0)
			light = get(lightWorldPos.move(0, 0, 2));
		return light;
	}

	private int get(BlockPos worldPos) {
		int index = indexIfInsideCache(worldPos);
		if (index == -1)
			return fetchCombinedLight(worldPos);

		int[] array = getArray();
		int light = array[index];
		if (light == -1)
			array[index] = light = fetchCombinedLight(worldPos);
		return light;
	}

	private int fetchCombinedLight(BlockPos worldPos) {
		BlockState state = world.getBlockState(worldPos);
		return LIGHTER.get().getLightCoords(state, world, worldPos);
	}

	private static final ThreadLocal<BlockModelLighter> LIGHTER = ThreadLocal.withInitial(BlockModelLighter::new);

	private int[] getArray() {
		int[] array = this.array;
		if (array == null)
			this.array = array = CACHE.takeArray(numBlocks());
		return array;
	}

	private int numBlocks() {
		BlockPos size = this.size;
		return size.getX() * size.getY() * size.getZ();
	}

	private int indexIfInsideCache(BlockPos worldPos) {
		BlockPos start = this.start;
		BlockPos size = this.size;
		int x = worldPos.getX() - start.getX();
		int y = worldPos.getY() - start.getY();
		int z = worldPos.getZ() - start.getZ();
		if (x < 0 || x >= size.getX() || y < 0 || y >= size.getY() || z < 0 || z >= size.getZ())
			return -1; // Outside cache
		return ModUtil.get3dIndexInto1dArray(x, y, z, size.getX(), size.getY());
	}

	@Override
	public void close() {
	}

}

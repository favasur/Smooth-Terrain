package io.github.favasur.smoothterrain.util;

import com.google.common.collect.ImmutableList;
import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.platform.IPlatform;
import io.github.favasur.smoothterrain.platform.PlatformLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * See also {@link io.github.favasur.smoothterrain.client.ClientUtil}
 */
public class ModUtil {

	public static final BlockPos VEC_ZERO = new BlockPos(0, 0, 0);
	public static final BlockPos VEC_ONE = new BlockPos(1, 1, 1);
	public static final BlockPos VEC_TWO = new BlockPos(2, 2, 2);
	public static final BlockPos VEC_THREE = new BlockPos(3, 3, 3);
	public static final BlockPos CHUNK_SIZE = new BlockPos(16, 16, 16);
	public static final Direction[] DIRECTIONS = Direction.values();
	public static final float FULLY_SMOOTHABLE = 1;
	public static final float NOT_SMOOTHABLE = -FULLY_SMOOTHABLE;
	public static final IPlatform platform = PlatformLoader.load(IPlatform.class);

	public static ImmutableList<BlockState> getStates(Block block) {
		return block.getStateDefinition().getPossibleStates();
	}

	public static int length(BlockPos size) {
		return size.getX() * size.getY() * size.getZ();
	}

	public static void warnPlayer(@Nullable Player player, String translationKey, Object... formatArgs) {
		if (player != null)
			player.sendSystemMessage(Component.translatable(translationKey, formatArgs).withStyle(ChatFormatting.RED));
		else
			LogManager.getLogger("SmoothTerrain notification fallback").warn(I18n.get(translationKey, formatArgs));
			// Language.getInstance().getOrDefault(translationKey).formatted
	}

	public static float getBlockDensity(Predicate<BlockState> isSmoothable, BlockState state) {
		return getBlockDensity(isSmoothable.test(state), state);
	}

	/**
	 * @return Positive density if the block is smoothable (and will be at least partially inside the isosurface)
	 */
	public static float getBlockDensity(boolean shouldSmooth, BlockState state) {
		if (shouldSmooth)
			return getSmoothBlockDensity(state);
		// Solid-render (full opaque) blocks - walls, planks, ores, logs, bricks, ... - are treated
		// as fully inside the shape so smooth surfaces clamp to their faces instead of receding
		// from them (treating them as air left ~0.3-block-wide empty gaps in front of solid
		// blocks). The check is the exact one the vanilla block-state cache uses, so registered
		// states answer it from the cached flag without touching the empty getter.
		if (platform.isSolidRender(state))
			return FULLY_SMOOTHABLE;
		return NOT_SMOOTHABLE;
	}

	public static float getSmoothBlockDensity(BlockState state) {
		if (isSnowLayer(state))
			// Snow layer, not the actual whole snow block
			return mapSnowHeight(state.getValue(SnowLayerBlock.LAYERS));
		return FULLY_SMOOTHABLE;
	}

	/** Map snow height between 1-8 to between -1 and 1. */
	private static float mapSnowHeight(int value) {
		return -1 + (value - 1) * 0.25F;
	}

	public static boolean isSnowLayer(BlockState state) {
		return state.hasProperty(SnowLayerBlock.LAYERS);
	}

	public static boolean isShortPlant(BlockState state) {
		Block block = state.getBlock();
		return block instanceof BushBlock && !(block instanceof DoublePlantBlock || block instanceof CropBlock || block instanceof StemBlock);
	}

	public static boolean isPlant(BlockState state) {
		return platform.isPlant(state);
	}

	/**
	 * Assumes the array is indexed [z][y][x].
	 */
	public static int get3dIndexInto1dArray(int x, int y, int z, int xSize, int ySize) {
		return (xSize * ySize * z) + (xSize * y) + x;
	}

	/**
	 * Searches neighbouring positions around a smooth block for a fluid state.
	 * Makes the smooth block have the fluid "extended" into it, so the receded
	 * smoothed surface at pool edges/bends stays covered instead of showing empty
	 * pockets. Any non-empty fluid qualifies (source or flowing): flowing pools
	 * (e.g. 1-wide cross channels, which never form sources) would otherwise leave
	 * the receded cells unfilled. The returned state is the fluid's default
	 * (source-level) state so the whole receded cell is covered.
	 *
	 * @return a fluid state that may not actually exist in the position
	 */
	public static FluidState getExtendedFluidState(Level world, BlockPos pos) {
		var extendRange = SmoothTerrainConfig.Server.extendFluidsRange;
		assert extendRange > 0;

		var x = pos.getX();
		var y = pos.getY();
		var z = pos.getZ();
		var chunkX = x >> 4;
		var chunkZ = z >> 4;
		var chunk = world.getChunk(chunkX, chunkZ);

		var blockState = chunk.getBlockState(pos);
		var fluidState = blockState.getFluidState();
		// If the fluid is non-empty we don't have to look for a fluid
		// If the state isn't smoothable we don't want to extend fluid into it
		if (!fluidState.isEmpty() || !SmoothTerrain.smoothableHandler.isSmoothable(blockState))
			return fluidState;

		// Check up
		fluidState = chunk.getFluidState(x, y + 1, z);
		if (!fluidState.isEmpty())
			return fluidState.getType().defaultFluidState();

		// Check around
		for (var extendZ = z - extendRange; extendZ <= z + extendRange; ++extendZ) {
			for (var extendX = x - extendRange; extendX <= x + extendRange; ++extendX) {
				if (extendZ == z && extendX == x)
					continue; // We already checked ourselves above

				if (chunkX != extendZ >> 4 || chunkZ != extendX >> 4) {
					chunkZ = extendZ >> 4;
					chunkX = extendX >> 4;
					chunk = world.getChunk(chunkX, chunkZ);
				}

				fluidState = chunk.getFluidState(extendX, y, extendZ);
				if (!fluidState.isEmpty())
					return fluidState.getType().defaultFluidState();
			}
		}
		return Fluids.EMPTY.defaultFluidState();
	}

}

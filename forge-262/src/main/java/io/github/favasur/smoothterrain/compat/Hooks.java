package io.github.favasur.smoothterrain.hooks;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.collision.CollisionHandler;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

import static net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;

/**
 * Contains logic that gets used by traits/mixins in the {@link io.github.favasur.smoothterrain.mixin} package.
 */
@SuppressWarnings("unused") // Called via ASM
public final class Hooks {

	public static boolean renderingEnabledFor(BlockStateBase state) {
		return SmoothTerrainConfig.Client.render && SmoothTerrain.smoothableHandler.isSmoothable(state);
	}

	public static boolean collisionsEnabledFor(BlockStateBase state) {
		return SmoothTerrainConfig.Server.collisionsEnabled && SmoothTerrain.smoothableHandler.isSmoothable(state);
	}

	/**
	 * Hooking this makes {@link Block#shouldRenderFace} return true and causes cubic terrain (including fluids) to be
	 * rendered when they are up against smooth terrain, stopping us from being able to see through the ground near
	 * smooth terrain.
	 */
	public static boolean shouldCancelOcclusion(BlockStateBase state) {
		return renderingEnabledFor(state);
	}

	/**
	 * Helper function for use by other hooks/mixins.
	 */
	public static boolean shapeOfSmoothBlockIntersectsEntityAABB(Entity entity, BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.joinIsNotEmpty(
			CollisionHandler.getShapeOfSmoothBlock(state, level, pos, CollisionContext.of(entity)).move(pos.getX(), pos.getY(), pos.getZ()),
			Shapes.create(entity.getBoundingBox()),
			BooleanOp.AND
		);
	}

}

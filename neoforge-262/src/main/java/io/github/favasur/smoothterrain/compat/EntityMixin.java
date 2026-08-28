package io.github.favasur.smoothterrain.compat;

import io.github.favasur.smoothterrain.collision.MeshCollisionScope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.2 port of the shared {@code io.github.favasur.smoothterrain.mixin.EntityMixin}: enters and
 * exits {@link MeshCollisionScope} around {@link Entity#collide(Vec3)} so that the 26.2
 * {@code net.buildertools.mixin.CollisionGetterMixin} only hands out triangle-backed mesh shapes
 * during genuine movement resolution. Camera, placement, suffocation and pathfinding queries keep
 * receiving ordinary VoxelShapes because the scope is not active for them. Also feeds the entity
 * into the suffocation collision lookup so mesh collisions work even when
 * {@code tempMobCollisionsDisabled} is false.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

	/**
	 * Exact mesh shapes are valid only while vanilla is resolving the block list for Entity#move.
	 */
	@Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"))
	private void smoothterrain$beginMovementCollision(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
		MeshCollisionScope.enterMovement();
	}

	@Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"))
	private void smoothterrain$endMovementCollision(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
		MeshCollisionScope.exitMovement();
	}

	/**
	 * The suffocation check queries collision shapes without passing the entity, so the collision
	 * getter would not receive the triangle mesh there. Provide the entity so wall checks see the
	 * mesh. In 26.2 the lambda synthetic is {@code lambda$isInWall$0} (verified from bytecode; the
	 * 1.21.1 build uses {@code lambda$isInWall$8}).
	 */
	@Redirect(
		method = "lambda$isInWall$0",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
		)
	)
	private VoxelShape smoothterrain$isInWall$getCollisionShape$entityAware(BlockState state, BlockGetter world, BlockPos pos) {
		return state.getCollisionShape(world, pos, CollisionContext.of((Entity)(Object)this));
	}

}

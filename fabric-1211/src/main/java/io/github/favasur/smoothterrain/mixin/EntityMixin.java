package io.github.favasur.smoothterrain.mixin;

import io.github.favasur.smoothterrain.collision.MeshCollisionScope;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric copy of the canonical Smooth Terrain {@code EntityMixin} movement scope. The canonical
 * mixin tree lives under the NeoForge-only {@code smoothterrain/mixin} sources, which this module
 * deliberately excludes; the movement scope is loader-agnostic and required for the rotated-block
 * collision layer (the {@code CollisionGetterMixin} only serves the exact mesh shapes while
 * {@code MeshCollisionScope.isEntityMovement()} is true), so it is supplied here.
 */
@Mixin(Entity.class)
public class EntityMixin {

    /**
     * Exact mesh shapes are valid only while vanilla is resolving the block list for Entity#move.
     * Keeping this scope around the private collision routine prevents camera, placement,
     * suffocation and pathfinding queries from receiving a triangle-backed VoxelShape.
     */
    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"))
    private void buildertools$beginMovementCollision(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        MeshCollisionScope.enterMovement();
    }

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"))
    private void buildertools$endMovementCollision(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        MeshCollisionScope.exitMovement();
    }
}

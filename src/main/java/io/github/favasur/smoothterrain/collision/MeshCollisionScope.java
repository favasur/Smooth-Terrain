package io.github.favasur.smoothterrain.collision;

/**
 * Marks the small portion of Minecraft's movement pipeline that is allowed to consume exact mesh
 * collision shapes.  The marker is thread-local because collision queries can also run for camera,
 * placement, pathfinding, suffocation, and entity spawning on the same game thread; those callers
 * must continue to receive ordinary block-local VoxelShapes.
 */
public final class MeshCollisionScope {
    private static final ThreadLocal<Integer> MOVEMENT_DEPTH = ThreadLocal.withInitial(() -> 0);

    private MeshCollisionScope() {
    }

    public static void enterMovement() {
        MOVEMENT_DEPTH.set(MOVEMENT_DEPTH.get() + 1);
    }

    public static void exitMovement() {
        int depth = MOVEMENT_DEPTH.get() - 1;
        if (depth <= 0) {
            MOVEMENT_DEPTH.remove();
        } else {
            MOVEMENT_DEPTH.set(depth);
        }
    }

    public static boolean isEntityMovement() {
        return MOVEMENT_DEPTH.get() > 0;
    }
}

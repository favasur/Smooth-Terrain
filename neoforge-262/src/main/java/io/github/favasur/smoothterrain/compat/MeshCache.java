package io.github.favasur.smoothterrain.mesh;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.util.Area;
import io.github.favasur.smoothterrain.util.Face;
import io.github.favasur.smoothterrain.util.ModUtil;
import io.github.favasur.smoothterrain.util.Vec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Caches the rendered smooth-terrain faces used by movement collision. */
public final class MeshCache {
    private static final class SectionMesh {
        final BlockPos origin;
        final BlockPos areaStart;
        volatile List<Face> faces;
        volatile MeshCollisionShape shape;

        SectionMesh(BlockPos origin, BlockPos areaStart) {
            this.origin = origin;
            this.areaStart = areaStart;
        }
    }

    private static final Map<String, Map<Long, SectionMesh>> CACHE = new ConcurrentHashMap<>();

    private MeshCache() {
    }

    public static MeshCollisionShape getCollisionShape(BlockGetter world, BlockPos sectionOrigin, Mesher mesher) {
        SectionMesh section = getSection(world, sectionOrigin, mesher);
        MeshCollisionShape result = section.shape;
        if (result == null) {
            synchronized (section) {
                result = section.shape;
                if (result == null) {
                    result = MeshCollisionShape.fromFaces(getCollisionFaces(world, section, mesher), section.areaStart);
                    section.shape = result;
                }
            }
        }
        return result;
    }

    private static List<Face> getCollisionFaces(BlockGetter world, SectionMesh section, Mesher mesher) {
        List<Face> result = section.faces;
        if (result == null) {
            synchronized (section) {
                result = section.faces;
                if (result == null) {
                    List<Face> faces = new ArrayList<>();
                    try (Area area = new Area(world, section.origin, ModUtil.CHUNK_SIZE, mesher)) {
                        mesher.generateGeometry(area, SmoothTerrain.smoothableHandler::isSmoothable, (pos, face) -> {
                            faces.add(copyFace(face));
                            return true;
                        });
                    }
                    result = List.copyOf(faces);
                    section.faces = result;
                }
            }
        }
        return result;
    }

    public static void invalidateAround(Level level, BlockPos pos) {
        // 26.2 renamed ResourceKey#location to #identifier.
        String prefix = level.dimension().identifier() + "|" + System.identityHashCode(level) + "|";
        int cx = pos.getX() >> 4;
        int cy = pos.getY() >> 4;
        int cz = pos.getZ() >> 4;
        for (Map.Entry<String, Map<Long, SectionMesh>> entry : CACHE.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            Map<Long, SectionMesh> sections = entry.getValue();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        sections.remove(SectionPos.asLong(cx + dx, cy + dy, cz + dz));
                    }
                }
            }
        }
    }

    public static void clear() {
        CACHE.clear();
    }

    private static SectionMesh getSection(BlockGetter world, BlockPos sectionOrigin, Mesher mesher) {
        String worldKey = world instanceof Level level
                ? level.dimension().identifier() + "|" + System.identityHashCode(level)
                : "?";
        long sectionKey = SectionPos.asLong(
                sectionOrigin.getX() >> 4, sectionOrigin.getY() >> 4, sectionOrigin.getZ() >> 4);
        String cacheKey = worldKey + "|" + mesher.cacheId();
        Map<Long, SectionMesh> sections = CACHE.computeIfAbsent(cacheKey, key -> new ConcurrentHashMap<>());
        return sections.computeIfAbsent(sectionKey, key -> {
            BlockPos areaStart;
            try (Area area = new Area(world, sectionOrigin, ModUtil.CHUNK_SIZE, mesher)) {
                areaStart = area.start;
            }
            return new SectionMesh(sectionOrigin.immutable(), areaStart);
        });
    }

    private static Face copyFace(Face face) {
        Face copy = new Face(new Vec(), new Vec(), new Vec(), new Vec());
        copy.setValuesFrom(face);
        return copy;
    }
}

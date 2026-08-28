package io.github.favasur.smoothterrain.config;

import io.github.favasur.smoothterrain.mesh.SurfaceNets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/** Fabric adapter for the canonical Smooth Terrain runtime settings. */
public final class SmoothTerrainConfigImpl {
    private SmoothTerrainConfigImpl() {
    }

    public static void register(ModContainer container, IEventBus modBus) {
        SmoothTerrainConfig.Server.mesher = new SurfaceNets(false);
        SmoothTerrainConfig.Server.collisionsEnabled = false;
        SmoothTerrainConfig.Server.forceVisuals = false;
        SmoothTerrainConfig.Client.render = false;
    }

    public static final class Server {
        private Server() {
        }

        public static void setEnabled(boolean enabled) {
            if (SmoothTerrainConfig.Server.mesher == null) {
                SmoothTerrainConfig.Server.mesher = new SurfaceNets(false);
            }
            SmoothTerrainConfig.Server.collisionsEnabled = enabled;
            SmoothTerrainConfig.Server.forceVisuals = enabled;
            SmoothTerrainConfig.Client.render = enabled;
        }

        /**
         * Runtime smoothness (extent of smoothness) update from the Video Settings slider: 1 is
         * perfectly smooth, 0 is maximally rough. Stored inverted as the mesher's roughness.
         */
        public static void setSmoothness(float smoothness) {
            SmoothTerrainConfig.Server.oldSmoothTerrainRoughness = 1F - Math.max(0F, Math.min(1F, smoothness));
        }
    }

    /**
     * Re-renders all chunks so a runtime setting change (Video Settings) is visible immediately.
     */
    public static void refreshRendering() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level != null) {
            minecraft.execute(minecraft.levelRenderer::allChanged);
        }
    }
}

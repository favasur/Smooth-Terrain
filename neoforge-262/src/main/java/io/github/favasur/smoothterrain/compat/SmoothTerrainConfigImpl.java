package io.github.favasur.smoothterrain.config;

import io.github.favasur.smoothterrain.mesh.SurfaceNets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

public final class SmoothTerrainConfigImpl {
    private SmoothTerrainConfigImpl() {
    }

    public static void register(ModContainer container, IEventBus modBus) {
        if (SmoothTerrainConfig.Server.mesher == null) {
            SmoothTerrainConfig.Server.mesher = new SurfaceNets(false);
        }
    }

    public static final class Server {
        private Server() {
        }

        public static void setEnabled(boolean enabled) {
            SmoothTerrainConfig.Server.mesher = new SurfaceNets(false);
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
     * The 26.2 modules share the vanilla section renderer for now, so the setting applies to the
     * next mesh build and there is nothing to invalidate here yet.
     */
    public static void refreshRendering() {
        io.github.favasur.smoothterrain.client.RenderHelper.reloadAllChunks("smooth terrain rendering refreshed");
    }
}

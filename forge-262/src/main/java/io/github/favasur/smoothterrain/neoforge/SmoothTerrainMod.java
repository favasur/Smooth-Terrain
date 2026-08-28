package io.github.favasur.smoothterrain.neoforge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge 26.2 entrypoint. Loader bootstrap for Smooth Terrain: registers the client config
 * and (no-op) network adapter, then wires the client keybindings and join messages.
 */
@Mod(SmoothTerrainMod.MODID)
public class SmoothTerrainMod {
    public static final String MODID = "smoothterrain";

    public SmoothTerrainMod() {
        io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl.register();
        io.github.favasur.smoothterrain.network.SmoothTerrainNetworkNeoForge.register();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.register();
        }
    }
}

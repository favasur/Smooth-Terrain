package io.github.favasur.smoothterrain.neoforge;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkNeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Standalone Smooth Terrain entry point (NeoForge 1.21.1). Registers the config, the payload
 * channel and the client-side hooks (key mappings, chunk-mesh renderer, server join sync).
 */
@Mod(SmoothTerrainMod.MOD_ID)
public final class SmoothTerrainMod {
    public static final String MOD_ID = "smoothterrain";

    public SmoothTerrainMod(IEventBus modEventBus, ModContainer container) {
        SmoothTerrainConfigImpl.register(container, modEventBus);
        SmoothTerrainNetworkNeoForge.register(modEventBus);

        if (FMLEnvironment.dist.isClient()) {
            ClientInit.register(modEventBus);
        }
    }
}

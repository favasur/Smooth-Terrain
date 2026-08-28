package io.github.favasur.smoothterrain.forge;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Standalone Smooth Terrain entry point (Forge 1.21.1). Registers the in-memory config and the
 * client-side hooks (key mappings, chunk-mesh renderer, join messages). The Forge copy has no
 * SmoothTerrain network channel, so the server config stays local.
 */
@Mod(SmoothTerrainMod.MOD_ID)
public final class SmoothTerrainMod {
    public static final String MOD_ID = "smoothterrain";

    public SmoothTerrainMod() {
        SmoothTerrainConfigImpl.register();

        if (FMLEnvironment.dist.isClient()) {
            ClientInit.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}

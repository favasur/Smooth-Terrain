package io.github.favasur.smoothterrain.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * NeoForge 26.2 entrypoint. Loader bootstrap for Smooth Terrain: registers the client config
 * and the (no-op) network adapter, then wires the client keybindings and join messages.
 */
@Mod(SmoothTerrainMod.MODID)
public class SmoothTerrainMod {
    public static final String MODID = "smoothterrain";

    public SmoothTerrainMod(IEventBus modEventBus, net.neoforged.fml.ModContainer container) {
        io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl.register(container, modEventBus);
        io.github.favasur.smoothterrain.network.SmoothTerrainNetworkNeoForge.register(modEventBus);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ClientInit.register(modEventBus);
        }
    }
}

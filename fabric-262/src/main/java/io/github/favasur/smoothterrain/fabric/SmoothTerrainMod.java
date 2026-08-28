package io.github.favasur.smoothterrain.fabric;

import net.fabricmc.api.ModInitializer;
import net.neoforged.bus.api.FabricEventBus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Fabric 26.2 main entrypoint for the Smooth Terrain spin-off: registers the client config
 * through the NeoForge-style shim layer (FabricEventBus + ModContainer) and the no-op network
 * adapter. Client-only wiring happens in {@link ClientInit} via the client entrypoint.
 */
public final class SmoothTerrainMod implements ModInitializer {
    public static final String MODID = "smoothterrain";

    /** Mod (loader) event bus; fired by the Fabric hook layer. */
    public static final IEventBus MOD_EVENT_BUS = new FabricEventBus();

    @Override
    public void onInitialize() {
        io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl.register(new ModContainer(), MOD_EVENT_BUS);
        SmoothTerrainNetworkFabric.register();
    }
}

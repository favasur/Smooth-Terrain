package io.github.favasur.smoothterrain.fabric;

import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric 1.21.1 client entrypoint: runs after {@link SmoothTerrainMod#onInitialize()}, wiring
 * the keybindings, join-time messages and client tick handlers.
 */
public final class SmoothTerrainModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientInit.register();
    }
}

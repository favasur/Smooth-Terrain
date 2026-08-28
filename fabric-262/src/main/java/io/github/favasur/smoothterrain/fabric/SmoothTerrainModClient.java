package io.github.favasur.smoothterrain.fabric;

import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric 26.2 client entrypoint: runs after {@link SmoothTerrainMod#onInitialize()}, wiring
 * the keybindings, join-time messages and client tick handlers.
 */
public final class SmoothTerrainModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientInit.register();
    }
}

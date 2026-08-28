package io.github.favasur.smoothterrain.network;

/**
 * Forge 26.2 host adapter. Smooth Terrain's full loader network implementation is excluded
 * from the copied source set because its 1.21.x payload and config lifecycle is incompatible;
 * Builder Tools owns the equivalent world-setting payload registration (see
 * {@code net.buildertools.network.ModPackets}).
 */
public final class SmoothTerrainNetworkNeoForge {
    private SmoothTerrainNetworkNeoForge() {
    }

    public static void register() {
        // The Builder Tools packet registrar handles the shared Smooth Terrain toggle payload.
    }
}

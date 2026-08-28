package io.github.favasur.smoothterrain.fabric;

import io.github.favasur.smoothterrain.client.KeyMappings;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientInit {
    private ClientInit() {
    }

    public static void register() {
        SmoothTerrainNetworkFabric.registerClient();
        // Smooth Terrain keybindings: registered directly through Fabric's KeyBindingHelper (the
        // mod-bus RegisterKeyMappingsEvent is fired later by FabricHooks, but the canonical
        // registration takes a Consumer<KeyMapping> so we can wire it immediately), with the
        // consume-click poller running on the game-bus client-tick shim.
        KeyMappings.register(
                KeyBindingHelper::registerKeyBinding,
                onTick -> NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, tick -> onTick.run())
        );
    }
}

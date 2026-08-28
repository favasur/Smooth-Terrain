package io.github.favasur.smoothterrain.neoforge;

import io.github.favasur.smoothterrain.client.ClientUtil;
import io.github.favasur.smoothterrain.client.KeyMappings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge 26.2 client adapter for the copied Smooth Terrain core: registers the keybindings
 * (visuals toggle + smoothable toggles, applied locally - the 26.2 copy has no SmoothTerrain
 * network channel, Builder Tools' world-setting toggle owns enable/disable) and sends the
 * join-time info/warning messages. The chunk mesh is drawn by the SectionCompilerMixin registered
 * in the smoothterrain mixin config.
 */
public final class ClientInit {
    private ClientInit() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            KeyMappings.register(e::register, onTick ->
                    NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post tickEvent) -> onTick.run()));
        });

        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
            ClientUtil.sendPlayerInfoMessage();
            ClientUtil.warnPlayerIfVisualsDisabled();
        });
    }
}

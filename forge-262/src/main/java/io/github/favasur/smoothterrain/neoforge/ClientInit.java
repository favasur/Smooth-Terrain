package io.github.favasur.smoothterrain.neoforge;

import io.github.favasur.smoothterrain.client.ClientUtil;
import io.github.favasur.smoothterrain.client.KeyMappings;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Forge 26.2 client adapter for the copied Smooth Terrain core: registers the keybindings
 * (visuals toggle + smoothable toggles, applied locally - the 26.2 copy has no SmoothTerrain
 * network channel, Builder Tools' world-setting toggle owns enable/disable) and sends the
 * join-time info/warning messages. The chunk mesh is drawn by the SectionCompilerMixin registered
 * in the smoothterrain mixin config.
 */
public final class ClientInit {
    private ClientInit() {
    }

    public static void register() {
        RegisterKeyMappingsEvent.BUS.addListener(e ->
                KeyMappings.register(e::register, onTick ->
                        TickEvent.ClientTickEvent.Post.BUS.addListener(tick -> onTick.run())));

        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> {
            ClientUtil.sendPlayerInfoMessage();
            ClientUtil.warnPlayerIfVisualsDisabled();
        });
    }
}

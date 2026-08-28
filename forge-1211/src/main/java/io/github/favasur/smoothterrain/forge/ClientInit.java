package io.github.favasur.smoothterrain.forge;

import io.github.favasur.smoothterrain.client.ClientUtil;
import io.github.favasur.smoothterrain.client.KeyMappings;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Forge 1.21.1 client adapter for the copied Smooth Terrain core: registers the keybindings
 * (visuals toggle + smoothable toggles, applied locally - the Forge copy has no SmoothTerrain
 * network channel) and sends the join-time info/warning messages. The chunk mesh is drawn by the
 * SectionCompilerMixin registered in the smoothterrain mixin config.
 */
public final class ClientInit {
    private ClientInit() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) ->
                KeyMappings.register(e::register, onTick ->
                        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post tickEvent) -> onTick.run())));

        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
            ClientUtil.sendPlayerInfoMessage();
            ClientUtil.warnPlayerIfVisualsDisabled();
        });
    }
}

package io.github.favasur.smoothterrain.fabric;

import io.github.favasur.smoothterrain.client.ClientUtil;
import io.github.favasur.smoothterrain.client.KeyMappings;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Fabric 26.2 client adapter for the copied Smooth Terrain core: registers the keybindings
 * (visuals toggle + smoothable toggles, applied locally - the 26.2 copy has no SmoothTerrain
 * network channel, Builder Tools' world-setting toggle owns enable/disable) and sends the
 * join-time info/warning messages. The chunk mesh is drawn by the SectionCompilerMixin registered
 * in the smoothterrain mixin config.
 */
public final class ClientInit {
    private ClientInit() {
    }

    public static void register() {
        KeyMappings.register(
                KeyMappingHelper::registerKeyMapping,
                onTick -> NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, tick -> onTick.run())
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientUtil.sendPlayerInfoMessage();
            ClientUtil.warnPlayerIfVisualsDisabled();
        });
    }
}

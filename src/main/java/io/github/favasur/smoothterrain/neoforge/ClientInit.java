package io.github.favasur.smoothterrain.neoforge;

import io.github.favasur.smoothterrain.client.KeyMappings;
import io.github.favasur.smoothterrain.client.render.VanillaRenderer;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Bundled into Builder Tools: registers the Smooth Terrain client-side hooks (key mappings, the
 * chunk-mesh renderer and the server join sync) under the host mod's lifecycle.
 */
public final class ClientInit {

	public static void register(IEventBus modBus) {
		modBus.addListener((RegisterKeyMappingsEvent e) -> {
			KeyMappings.register(e::register, onTick ->
				NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post tickEvent) -> onTick.run())
			);
		});

		// Custom chunk geometry: NeoForge fires AddSectionGeometryEvent for every section during
		// SectionCompiler.compile (the same point the old SectionCompilerMixin hooked). When smooth
		// terrain rendering is enabled we register a renderer that draws the Surface-Nets mesh into
		// the section's buffers via the context's per-layer VertexConsumers.
		NeoForge.EVENT_BUS.addListener((AddSectionGeometryEvent event) -> {
			if (!SmoothTerrainConfig.Client.render)
				return;
			event.addRenderer(ctx -> VanillaRenderer.renderChunk(event.getSectionOrigin(), ctx));
		});

		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
			SmoothTerrainNetworkClient.onJoinedServer(event.getConnection().isMemoryConnection());
		});
	}
}

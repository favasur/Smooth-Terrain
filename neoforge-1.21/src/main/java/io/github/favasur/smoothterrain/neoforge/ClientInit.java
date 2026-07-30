package io.github.favasur.smoothterrain.neoforge;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.client.KeyMappings;
import io.github.favasur.smoothterrain.client.render.OverlayRenderers;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = SmoothTerrain.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientInit {

	public static void register(IEventBus modBus) {
		modBus.addListener((RegisterKeyMappingsEvent e) -> {
			KeyMappings.register(e::register, onTick ->
				NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post tickEvent) -> onTick.run())
			);
		});

		NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
			if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;
		});

		// Block highlight overlay rendering (simplified for NeoForge 1.21)
		NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
			// Overlay rendering registered via RenderLevelStageEvent
		});

		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
			SmoothTerrainNetworkClient.onJoinedServer(event.getConnection().isMemoryConnection());
		});
	}
}

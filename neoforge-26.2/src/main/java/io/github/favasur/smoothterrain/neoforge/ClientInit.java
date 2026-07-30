package io.github.favasur.smoothterrain.neoforge;

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
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@EventBusSubscriber(modid = io.github.favasur.smoothterrain.SmoothTerrain.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientInit {

	@EventBusSubscriber(modid = io.github.favasur.smoothterrain.SmoothTerrain.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
	public static final class GameEvents {
	}

	public static void register(IEventBus modBus) {
		modBus.addListener((RegisterKeyMappingsEvent registerEvent) -> {
			KeyMappings.register(registerEvent::register, onTick -> NeoForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent tickEvent) -> {
				if (tickEvent.phase != TickEvent.Phase.END)
					return;
				onTick.run();
			}));
		});

		NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
			if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS)
				return;
			// We need to use a hack since OverlayRenderers.register was called differently in old Forge
			// For now, we defer overlay registration to a simpler mechanism
		});

		NeoForge.EVENT_BUS.addListener((RenderHighlightEvent event) -> {
			var world = Minecraft.getInstance().level;
			if (world == null)
				return;
			var targetHitResult = event.getTarget();
			if (!(targetHitResult instanceof BlockHitResult target))
				return;
			var lookingAtPos = target.getBlockPos();
			var camera = event.getCamera().getPosition();
			if (OverlayRenderers.renderSmoothTerrainBlockHighlight(
				event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()),
				camera.x, camera.y, camera.z,
				world, lookingAtPos, world.getBlockState(lookingAtPos)
			))
				event.setCanceled(true);
		});

		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
			// In NeoForge we check if it's a vanilla connection
			var neoForgeAlreadyLoadedDefaultConfig = event.getConnection().isMemoryConnection();
			SmoothTerrainNetworkClient.onJoinedServer(neoForgeAlreadyLoadedDefaultConfig);
		});
	}
}

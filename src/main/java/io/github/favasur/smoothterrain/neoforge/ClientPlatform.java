package io.github.favasur.smoothterrain.neoforge;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import io.github.favasur.smoothterrain.network.C2SRequestUpdateSmoothable;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkNeoForge;
import io.github.favasur.smoothterrain.platform.IClientPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class ClientPlatform implements IClientPlatform {
	@Override
	public void updateClientVisuals(boolean render) {
		SmoothTerrainConfigImpl.Client.updateRender(render);
	}

	@Override
	public void sendC2SRequestUpdateSmoothable(boolean newValue, BlockState[] states) {
		SmoothTerrainNetworkNeoForge.sendToServer(new C2SRequestUpdateSmoothable(newValue, states));
	}

	@Override
	public void loadDefaultServerConfig() {
		SmoothTerrainConfigImpl.Hacks.loadDefaultServerConfig();
	}

	@Override
	public void receiveSyncedServerConfig(byte[] configData) {
		SmoothTerrainConfigImpl.Hacks.receiveSyncedServerConfig(configData);
	}

	@Override
	public Component clientConfigComponent() {
		var configFile = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve(SmoothTerrain.MOD_ID + "-client.toml").toFile();
		return Component.literal(configFile.getName())
			.withStyle(ChatFormatting.UNDERLINE)
			.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, configFile.getAbsolutePath())));
	}

	@Override
	public void forEachRenderLayer(BlockState state, Consumer<RenderType> action) {
		var layers = ItemBlockRenderTypes.getRenderLayers(state);
		for (var layer : layers) {
			action.accept(layer);
		}
	}

	@Override
	public List<BakedQuad> getQuads(BakedModel model, BlockState state, Direction direction, RandomSource random, Object modelData, RenderType layer) {
		return model.getQuads(state, direction, random);
	}
}

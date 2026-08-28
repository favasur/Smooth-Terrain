package io.github.favasur.smoothterrain.client;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.platform.IClientPlatform;
import io.github.favasur.smoothterrain.platform.PlatformLoader;
import io.github.favasur.smoothterrain.util.ModUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class ClientUtil {

	public static final IClientPlatform platform = PlatformLoader.load(IClientPlatform.class);

	public static void warnPlayer(String translationKey, Object... formatArgs) {
		ModUtil.warnPlayer(Minecraft.getInstance().player, translationKey, formatArgs);
	}

	public static FluidState getExtendedFluidState(BlockPos pos) {
		var level = Minecraft.getInstance().level;
		return level == null ? Fluids.EMPTY.defaultFluidState() : ModUtil.getExtendedFluidState(level, pos);
	}

	public static void warnPlayerIfVisualsDisabled() {
		if (!SmoothTerrainConfig.Client.render)
			warnPlayer(
				SmoothTerrain.MOD_ID + ".notification.visualsDisabled",
				KeyMappings.translate(KeyMappings.TOGGLE_VISUALS),
				SmoothTerrainConfig.Client.RENDER,
				platform.clientConfigComponent()
			);
	}

	public static void sendPlayerInfoMessage() {
		if (SmoothTerrainConfig.Client.infoMessage)
			Minecraft.getInstance().player.sendSystemMessage(Component.translatable(
				SmoothTerrain.MOD_ID + ".notification.infoMessage",
				KeyMappings.translate(KeyMappings.TOGGLE_SMOOTHABLE_BLOCK_TYPE),
				SmoothTerrainConfig.Client.INFO_MESSAGE,
				platform.clientConfigComponent()
			).withStyle(ChatFormatting.GREEN));
	}
}

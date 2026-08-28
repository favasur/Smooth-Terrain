package io.github.favasur.smoothterrain.neoforge;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import io.github.favasur.smoothterrain.platform.IPlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BushBlock;

public class Platform implements IPlatform {
	@Override
	public ResourceLocation getRegistryName(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block);
	}

	@Override
	public boolean isPlant(BlockState state) {
		return state.getBlock() instanceof BushBlock;
	}

	@Override
	public void updateServerConfigSmoothable(boolean newValue, BlockState... states) {
		SmoothTerrainConfigImpl.Server.updateSmoothable(newValue, states);
	}
}

package io.github.favasur.smoothterrain.neoforge;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.platform.IPlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class Platform implements IPlatform {
    @Override
    public Identifier getRegistryName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    @Override
    public boolean isPlant(BlockState state) {
        return state.getBlock() instanceof BushBlock;
    }

    @Override
    public void updateServerConfigSmoothable(boolean newValue, BlockState... states) {
        for (BlockState state : states) {
            SmoothTerrainConfig.Smoothables.addDefault(state);
        }
    }
}

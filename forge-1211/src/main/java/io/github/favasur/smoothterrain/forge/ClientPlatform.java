package io.github.favasur.smoothterrain.forge;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import io.github.favasur.smoothterrain.platform.IClientPlatform;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;
import java.util.function.Consumer;

public final class ClientPlatform implements IClientPlatform {
    @Override
    public void updateClientVisuals(boolean render) {
        SmoothTerrainConfig.Client.render = render;
    }

    @Override
    public void sendC2SRequestUpdateSmoothable(boolean newValue, BlockState[] states) {
        // No Smooth Terrain network channel on Forge 1.21.1; toggles are applied locally.
    }

    @Override
    public void loadDefaultServerConfig() {
        SmoothTerrainConfigImpl.Server.setEnabled(false);
    }

    @Override
    public void receiveSyncedServerConfig(byte[] configData) {
        // No network channel; the local config is authoritative.
    }

    @Override
    public Component clientConfigComponent() {
        return Component.literal("Forge config");
    }

    @Override
    public void forEachRenderLayer(BlockState state, Consumer<RenderType> action) {
        action.accept(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    @Override
    public List<BakedQuad> getQuads(BakedModel model, BlockState state, Direction direction,
                                    RandomSource random, Object modelData, RenderType layer) {
        // Forge's BakedModel extension overload takes ModelData + RenderType.
        return model.getQuads(state, direction, random, (ModelData) modelData, layer);
    }
}

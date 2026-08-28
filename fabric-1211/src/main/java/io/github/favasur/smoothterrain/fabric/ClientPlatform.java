package io.github.favasur.smoothterrain.fabric;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import io.github.favasur.smoothterrain.network.C2SRequestUpdateSmoothable;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkClient;
import io.github.favasur.smoothterrain.platform.IClientPlatform;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Consumer;

import static io.github.favasur.smoothterrain.client.RenderHelper.reloadAllChunks;

public final class ClientPlatform implements IClientPlatform {
    @Override
    public void updateClientVisuals(boolean render) {
        SmoothTerrainConfig.Client.render = render;
    }

    @Override
    public void sendC2SRequestUpdateSmoothable(boolean newValue, BlockState[] states) {
        PacketDistributor.sendToServer(new C2SRequestUpdateSmoothable(newValue, states));
    }

    @Override
    public void loadDefaultServerConfig() {
        SmoothTerrainConfigImpl.Server.setEnabled(false);
    }

    @Override
    public void receiveSyncedServerConfig(byte[] configData) {
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(configData));
        SmoothTerrainConfig.Server.collisionsEnabled = buffer.readBoolean();
        SmoothTerrainConfig.Server.forceVisuals = buffer.readBoolean();
        SmoothTerrainConfig.Server.extendFluidsRange = buffer.readInt();
        SmoothTerrainConfig.Server.oldSmoothTerrainRoughness = buffer.readFloat();
        if (SmoothTerrainConfig.Server.forceVisuals) {
            SmoothTerrainConfig.Client.render = true;
        }
        SmoothTerrainNetworkClient.currentServerHasSmoothTerrain = true;
        reloadAllChunks("received synced server config from the server");
    }

    @Override
    public Component clientConfigComponent() {
        return Component.literal("Fabric config");
    }

    @Override
    public void forEachRenderLayer(BlockState state, Consumer<RenderType> action) {
        action.accept(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    @Override
    public List<BakedQuad> getQuads(BakedModel model, BlockState state, Direction direction,
                                    RandomSource random, Object modelData, RenderType layer) {
        return model.getQuads(state, direction, random);
    }
}

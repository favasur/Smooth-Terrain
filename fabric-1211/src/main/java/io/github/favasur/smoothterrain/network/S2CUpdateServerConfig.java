package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Fabric adaptation of the canonical S2C server-config payload. The canonical {@code create}
 * reads the NeoForge TOML config file; Fabric has no file-backed config, so the server serializes
 * its in-memory server settings instead. The record, codec and client handler are identical.
 */
public record S2CUpdateServerConfig(
	byte[] data
) implements CustomPacketPayload {

	public static final Type<S2CUpdateServerConfig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SmoothTerrain.MOD_ID, "s2c_update_server_config"));

	public static final StreamCodec<RegistryFriendlyByteBuf, S2CUpdateServerConfig> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BYTE_ARRAY, S2CUpdateServerConfig::data,
		S2CUpdateServerConfig::new
	);

	/**
	 * Serializes the current in-memory server config. Only the fields that meaningfully change
	 * between worlds/players are sent; the smoothable lists are synced per-toggle through
	 * {@link S2CUpdateSmoothable} instead.
	 */
	public static S2CUpdateServerConfig create() {
		var buffer = new FriendlyByteBuf(Unpooled.buffer());
		buffer.writeBoolean(SmoothTerrainConfig.Server.collisionsEnabled);
		buffer.writeBoolean(SmoothTerrainConfig.Server.forceVisuals);
		buffer.writeInt(SmoothTerrainConfig.Server.extendFluidsRange);
		buffer.writeFloat(SmoothTerrainConfig.Server.oldSmoothTerrainRoughness);
		var data = new byte[buffer.readableBytes()];
		buffer.readBytes(data);
		return new S2CUpdateServerConfig(data);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(S2CUpdateServerConfig msg, IPayloadContext ctx) {
		ctx.enqueueWork(() -> SmoothTerrainNetworkClient.handleS2CUpdateServerConfig(
			runnable -> runnable.run(), msg.data()
		));
	}
}

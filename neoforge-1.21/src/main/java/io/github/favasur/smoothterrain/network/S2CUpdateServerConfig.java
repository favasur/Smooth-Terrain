package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.SmoothTerrain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;
import java.nio.file.Files;

public record S2CUpdateServerConfig(
	byte[] data
) implements CustomPacketPayload {

	public static final Type<S2CUpdateServerConfig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SmoothTerrain.MOD_ID, "s2c_update_server_config"));

	public static final StreamCodec<RegistryFriendlyByteBuf, S2CUpdateServerConfig> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BYTE_ARRAY, S2CUpdateServerConfig::data,
		S2CUpdateServerConfig::new
	);

	public static S2CUpdateServerConfig create(ModConfig serverConfig) {
		assert FMLEnvironment.dist.isDedicatedServer() : "This should not be called on clients";
		try {
			var file = FMLPaths.CONFIGDIR.get().resolve(serverConfig.getFileName()).toFile();
			var data = Files.readAllBytes(file.toPath());
			return new S2CUpdateServerConfig(data);
		} catch (IOException e) {
			throw new RuntimeException("Could not read SmoothTerrain server config file!", e);
		}
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
